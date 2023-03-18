(ns milquetoast.client
  (:require [clojure.core.async :as async :refer [go go-loop <! >!]]
            [clojure.data.json :as json])
  (:import [org.eclipse.paho.client.mqttv3 MqttClient MqttConnectOptions MqttMessage IMqttMessageListener]
           org.eclipse.paho.client.mqttv3.persist.MemoryPersistence))

(defn- create-mqtt-client! [broker-uri username password]
  (let [client-id (MqttClient/generateClientId)
        opts (doto (MqttConnectOptions.)
               (.setCleanSession true)
               (.setAutomaticReconnect true)
               (.setPassword (char-array password))
               (.setUserName username))]
    (doto (MqttClient. broker-uri client-id (MemoryPersistence.))
      (.connect opts))))

(defn- create-message
  [msg {:keys [qos retain]
        :or {qos    1
             retain false}}]
  (doto (MqttMessage. (.getBytes msg))
    (.setQos      qos)
    (.setRetained retain)))

(defn- parse-message [mqtt-msg]
  {
   :id        (.getId mqtt-msg)
   :qos       (.getQos mqtt-msg)
   :retained  (.isRetained mqtt-msg)
   :duplicate (.isDuplicate mqtt-msg)
   :payload   (.toString mqtt-msg)
   })

(defprotocol IMilquetoastClient
  (send-message!    [_ topic msg opts])
  (add-channel!     [_ chan])
  (stop!            [_])
  (subscribe-topic! [_ topic opts]))

(defrecord MilquetoastClient [client open-channels verbose]
  IMilquetoastClient
  (send-message! [_ topic msg opts]
    (.publish client topic (create-message msg opts)))
  (stop! [_]
    (when verbose
      (println
       (str "stopping " (count @open-channels) " channels")))
    (doseq [chan @open-channels]
      (go (>! chan :stop)
          (async/close! chan)))
    true)
  (add-channel! [_ chan]
    (swap! open-channels conj chan))
  (subscribe-topic! [self topic opts]
    (let [{:keys [buffer-size]} opts
          chan (async/chan buffer-size)]
      (add-channel! self chan)
      (.subscribe client topic
                  (proxy [IMqttMessageListener] []
                    (messageArrived [topic mqtt-message]
                      (go (>! chan (assoc (parse-message mqtt-message)
                                          :topic topic))))))
      chan)))

(defn- parallelism []
  (-> (Runtime/getRuntime)
      (.availableProcessors)
      (+ 1)))

(defn pipe [in xf]
  (let [out (async/chan)]
    (async/pipeline (parallelism) out xf in)))

(defrecord MilquetoastJsonClient [client]
  IMilquetoastClient
  (send-message! [_ topic msg opts]
    (.publish client topic (create-message (json/write-str msg) opts)))
  (stop! [_] (stop! client))
  (add-channel! [_ chan] (add-channel! client chan))
  (subscribe-topic! [_ topic opts]
    (pipe (subscribe-topic! client topic opts)
          (fn [msg] (update msg :payload json/read-str)))))

(defn send! [client topic msg & {:keys [qos retain]
                                 :or   {qos 1 retain false}}]
  (send-message! client topic msg {:qos qos :retain retain}))

(defn open-channel!
  [client topic & {:keys [buffer-size qos retain]
                   :or   {buffer-size 1
                          qos         1
                          retain      false}}]
  (let [chan (async/chan buffer-size)]
    (add-channel! client chan)
    (go-loop [msg (<! chan)]
      (when (not= :stop msg)
        (send-message! client topic msg {:qos qos :retain retain})
        (recur (<! chan))))
    chan))

(defn subscribe! [client topic & {:keys [buffer-size]
                                  :or   {buffer-size 1}}]
  (subscribe-topic! client topic {:buffer-size buffer-size}))

(defn connect! [& {:keys [broker-uri username password verbose]
                   :or   {verbose false}}]
  (MilquetoastClient. (create-mqtt-client! broker-uri username password)
                      (atom [])
                      verbose))

(defn connect-json! [& args]
  (MilquetoastJsonClient. (apply connect! args)))
