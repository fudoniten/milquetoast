(ns milquetoast.client
  (:require [clojure.core.async :as async :refer [go go-loop <! >! alts!! <!! timeout]]
            [clojure.data.json :as json])
  (:import [org.eclipse.paho.client.mqttv3 MqttClient MqttConnectOptions MqttMessage IMqttMessageListener]
           org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
           java.time.Instant))

(defn- create-mqtt-client! [& {:keys [broker-uri username password]}]
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

(defn- retry-attempt [verbose f reconnect]
  (let [wrapped-attempt (fn []
                          (try [true (f)]
                               (catch RuntimeException e
                                 (do (when verbose
                                       (println (format "exception: %s"
                                                        (.toString e))))
                                     [false e]))))
        max-wait     (* 5 60 1000)] ;; wait at most 5 minutes
    (loop [[success? result] (wrapped-attempt)
           wait-ms 1000]
      (if success?
        result
        (do (when verbose
              (println (format "attempt failed, attempting reconnect")))
            (reconnect)
            (when verbose
              (println (format "sleeping %s ms" wait-ms)))
            (<!! (timeout wait-ms))
            (recur (wrapped-attempt) (min (* wait-ms 1.25) max-wait)))))))

(defn- create-message
  [msg {:keys [qos retain]
        :or {qos    1
             retain false}}]
  (doto (MqttMessage. (.getBytes msg))
    (.setQos      qos)
    (.setRetained retain)))

(defn- parse-message [mqtt-msg]
  {:id        (.getId mqtt-msg)
   :qos       (.getQos mqtt-msg)
   :retained  (.isRetained mqtt-msg)
   :duplicate (.isDuplicate mqtt-msg)
   :payload   (.toString mqtt-msg)
   :payload-bytes (.getPayload mqtt-msg)})

(defprotocol IMilquetoastClient
  (send-message!    [_ topic msg opts])
  (add-channel!     [_ chan])
  (stop!            [_])
  (subscribe-topic! [_ topic opts])
  (get-topic!       [_ topic opts])
  (get-topic-raw!   [_ topic opts]))

(defrecord MilquetoastClient [client open-channels verbose]
  IMilquetoastClient
  (send-message! [_ topic msg opts]
    (retry-attempt verbose
                   #(.publish client topic (create-message msg opts))
                   #(.reconnect client)))
  (stop! [_]
    (when verbose
      (println
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

(defn- parallelism []
  (-> (Runtime/getRuntime)
      (.availableProcessors)
      (+ 1)))

(defn pipe [in xf]
  (let [out (async/chan)]
    (async/pipeline (parallelism) out xf in)
    out))

(defn- json-parse-message [msg]
  (-> msg
      (update :payload   (fn [payload]
                           (json/read-str payload :key-fn keyword)))
      (assoc  :timestamp (Instant/now))))

(defrecord MilquetoastJsonClient [client]
  IMilquetoastClient
  (send-message! [_ topic msg opts]
    (send-message! client topic (json/write-str msg) opts))
  (stop! [_] (stop! client))
  (add-channel! [_ chan] (add-channel! client chan))
  (subscribe-topic! [_ topic opts]
    (pipe (subscribe-topic! client topic opts)
          (map json-parse-message)))
  (get-topic! [_ topic opts]
    (if-let [msg (get-topic! client topic opts)]
      (json-parse-message msg)
      nil))
  (get-topic-raw! [_ topic opts]
    (if-let [msg (get-topic! client topic opts)]
      msg
      nil)))

(defn send! [client topic msg & {:keys [qos retain]
                                 :or   {qos 1 retain false}}]
  (send-message! client topic msg {:qos qos :retain retain}))

(defn get! [client topic & options]
  (get-topic! client topic options))

(defn get-raw! [client topic & options]
  (get-topic-raw! client topic options))

(defn open-channel!
  [client topic & {:keys [buffer-size qos retain]
                   :or   {buffer-size 1
                          qos         1
                          retain      false}}]
  (let [chan (async/chan buffer-size)]
    (add-channel! client chan)
    (go-loop [msg (<! chan)]
      (when msg
        (send-message! client topic msg {:qos qos :retain retain})
        (recur (<! chan))))
    chan))

(defn subscribe!
  [client topic & {:keys [buffer-size qos]
                   :or   {buffer-size 1
                          qos         1}}]
  (subscribe-topic! client topic {:buffer-size buffer-size :qos qos}))

(defn connect!
  [& {:keys [host port scheme verbose]
      :or   {verbose false
             scheme  :tcp}
      :as   opts}]
  (let [broker-uri (str (name scheme) "://" host ":" port)]
    (MilquetoastClient. (create-mqtt-client! (assoc opts :broker-uri broker-uri))
                        (atom [])
                        verbose)))

(defn connect-json! [& args]
  (MilquetoastJsonClient. (apply connect! args)))
