(ns milquetoast.api
  (:require [clojure.core.async :refer [chan <! go-loop]]

            [milquetoast.core :refer [create-client create-json-client]]
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
  (let [chan (chan buffer-size)]
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
  "Connects to the MQTT broker with the provided host, port, username, password, scheme, and verbosity level.
  Returns a new MilquetoastClient instance."
  [& {:keys [host port username password scheme verbose]
      :or   {verbose false
             scheme  :tcp}}]
  (let [broker-uri (str (name scheme) "://" host ":" port)]
    (create-client :broker-uri broker-uri
                   :username   username
                   :password   password
                   :verbose    verbose)))

(defn connect-json!
  "Connects to the MQTT broker with the provided host, port, username, password, scheme, and verbosity level.
  Returns a new MilquetoastJsonClient instance."
  [& {:keys [host port username password scheme verbose]
      :or   {verbose false
             scheme  :tcp}}]
  (let [broker-uri (str (name scheme) "://" host ":" port)]
    (create-json-client :broker-uri broker-uri
                        :username   username
                        :password   password
                        :verbose    verbose)))
