(ns milquetoast.core
  (:require [clojure.core.async :as async :refer [go go-loop <! >! alts!! <!! timeout]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log])
  (:import [org.eclipse.paho.client.mqttv3 MqttClient MqttConnectOptions MqttMessage IMqttMessageListener]
           org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
           java.time.Instant))

(defn create-mqtt-client!
  "Creates and connects an MQTT client with the provided broker URI, username, and password."
  [& {:keys [broker-uri username password]}]
  (let [client-id (MqttClient/generateClientId)
        opts (doto (MqttConnectOptions.)
               (.setCleanSession true)
               (.setAutomaticReconnect true))]
    (when username
      (doto opts
        (.setUserName username)
        (.setPassword (char-array password))))
    (doto (MqttClient. broker-uri client-id (MemoryPersistence.))
      (.connect opts))))

(defn retry-attempt
  "Attempts to execute function `f`. If `f` throws a RuntimeException, logs the exception and attempts to reconnect before retrying `f`."
  [verbose f reconnect]
  (let [wrapped-attempt (fn []
                          (try [true (f)]
                               (catch RuntimeException e
                                 (do (when verbose
                                       (log/error e "exception"))
                                     [false e]))))
        max-wait     (* 5 60 1000)] ;; wait at most 5 minutes
    (loop [[success? result] (wrapped-attempt)
           wait-ms 1000]
      (if success?
        result
        (do (when verbose
              (log/warn "attempt failed, attempting reconnect"))
            (reconnect)
            (when verbose
              (log/debug (format "sleeping %s ms" wait-ms)))
            (<!! (timeout wait-ms))
            (recur (wrapped-attempt) (min (* wait-ms 1.25) max-wait)))))))

(defn create-message
  "Creates an MQTT message with the provided message string and options."
  [msg {:keys [qos retain]
        :or {qos    1
             retain false}}]
  (doto (MqttMessage. (.getBytes msg))
    (.setQos      qos)
    (.setRetained retain)))

(defn parse-message
  "Parses an MQTT message into a map with keys :id, :qos, :retained, :duplicate, :payload, and :payload-bytes."
  [mqtt-msg]
  {:id        (.getId mqtt-msg)
   :qos       (.getQos mqtt-msg)
   :retained  (.isRetained mqtt-msg)
   :duplicate (.isDuplicate mqtt-msg)
   :payload   (.toString mqtt-msg)
   :payload-bytes (.getPayload mqtt-msg)})

(defprotocol IMilquetoastClient
  "Protocol defining the core operations of a Milquetoast MQTT client."
  (send-message!    [_ topic msg opts])
  (add-channel!     [_ chan])
  (stop!            [_])
  (subscribe-topic! [_ topic opts])
  (get-topic!       [_ topic opts])
  (get-topic-raw!   [_ topic opts]))

(defrecord MilquetoastClient
  [client open-channels verbose]
  IMilquetoastClient
  (send-message! [_ topic msg opts]
    (retry-attempt verbose
                   #(.publish client topic (create-message msg opts))
                   #(.reconnect client)))
  (stop! [_]
    (when verbose
      (log/info
       (str "stopping " (count @open-channels) " channels")))
    (doseq [chan @open-channels]
      (async/close! chan))
    true)
  (add-channel! [_ chan]
    (swap! open-channels conj chan))
  (subscribe-topic! [self topic opts]
    (let [{:keys [buffer-size qos]
           :or   {buffer-size 1 qos 0}} opts
          chan (async/chan buffer-size)]
      (add-channel! self chan)
      (retry-attempt verbose
                     #(.subscribe client topic qos
                                  (proxy [IMqttMessageListener] []
                                    (messageArrived [topic mqtt-message]
                                      (go (>! chan (assoc (parse-message mqtt-message)
                                                          :topic topic))))))
                     #(.reconnect client))
      chan))
  (get-topic! [_ topic opts]
    (let [{:keys [qos timeout] :or {qos 0 timeout 5}} opts
          result-chan (async/chan)]
      (retry-attempt verbose
                     #(.subscribe client topic qos
                                  (proxy [IMqttMessageListener] []
                                    (messageArrived [topic mqtt-message]
                                      (go (>! result-chan (assoc (parse-message mqtt-message)
                                                                 :topic topic))
                                          (async/close! result-chan))
                                      (.unsubscribe client topic))))
                     #(.reconnect client))
      (first (alts!! [result-chan
                      (async/timeout (* timeout 1000))]))))
  (get-topic-raw! [c topic opts] (get-topic! c topic opts)))

(defrecord MilquetoastJsonClient
  [client]
  IMilquetoastClient
  (send-message! [_ topic msg opts]
    (send-message! client topic (json/write-str msg) opts))
  (stop! [_] (stop! client))
  (add-channel! [_ chan] (add-channel! client chan))
  (subscribe-topic! [_ topic opts]
    (pipe (subscribe-topic! client topic opts)
          (map utils/json-parse-message)))
  (get-topic! [_ topic opts]
    (if-let [msg (get-topic! client topic opts)]
      (utils/json-parse-message msg)
      nil))
  (get-topic-raw! [_ topic opts]
    (if-let [msg (get-topic! client topic opts)]
      msg
      nil)))
