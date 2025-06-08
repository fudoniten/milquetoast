(ns milquetoast.api)
(ns milquetoast.api
  (:require [milquetoast.core :as core]
            [milquetoast.utils :as utils]))

(defn send!
  "Sends a message to a topic on the provided client with the specified QoS and retain options."
  [client topic msg & {:keys [qos retain]
                                 :or   {qos 1 retain false}}]
  (core/send-message! client topic msg {:qos qos :retain retain}))

(defn get!
  "Gets a message from a topic on the provided client with the specified options."
  [client topic & options]
  (core/get-topic! client topic options))

(defn get-raw!
  "Gets a raw message from a topic on the provided client with the specified options."
  [client topic & options]
  (core/get-topic-raw! client topic options))

(defn open-channel!
  "Opens a channel for sending messages to a topic on the provided client with the specified buffer size, QoS, and retain options."
  [client topic & {:keys [buffer-size qos retain]
                   :or   {buffer-size 1
                          qos         1
                          retain      false}}]
  (let [chan (async/chan buffer-size)]
    (core/add-channel! client chan)
    (go-loop [msg (<! chan)]
      (when msg
        (core/send-message! client topic msg {:qos qos :retain retain})
        (recur (<! chan))))
    chan))

(defn subscribe!
  "Subscribes to a topic on the provided client with the specified buffer size and QoS options."
  [client topic & {:keys [buffer-size qos]
                   :or   {buffer-size 1
                          qos         1}}]
  (core/subscribe-topic! client topic {:buffer-size buffer-size :qos qos}))

(defn connect!
  "Connects to an MQTT broker at the provided host and port with the specified scheme and verbosity options."
  [& {:keys [host port scheme verbose]
      :or   {verbose false
             scheme  :tcp}
      :as   opts}]
  (let [broker-uri (str (name scheme) "://" host ":" port)]
    (core/MilquetoastClient. (core/create-mqtt-client! (assoc opts :broker-uri broker-uri))
                        (atom [])
                        verbose)))

(defn connect-json!
  "Connects to an MQTT broker with the provided arguments and configures the client to send and receive JSON messages."
  [& args]
  (core/MilquetoastJsonClient. (apply connect! args)))
