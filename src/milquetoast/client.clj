(ns milquetoast.client
  (:require [milquetoast.api :as api]))

(defn send! [client topic msg & {:keys [qos retain]
                                 :or   {qos 1 retain false}}]
  (api/send! client topic msg :qos qos :retain retain))

(defn get! [client topic & options]
  (api/get! client topic options))

(defn get-raw! [client topic & options]
  (api/get-raw! client topic options))

(defn open-channel!
  [client topic & {:keys [buffer-size qos retain]
                   :or   {buffer-size 1
                          qos         1
                          retain      false}}]
  (api/open-channel! client topic :buffer-size buffer-size :qos qos :retain retain))

(defn subscribe!
  [client topic & {:keys [buffer-size qos]
                   :or   {buffer-size 1
                          qos         1}}]
  (api/subscribe! client topic :buffer-size buffer-size :qos qos))

(defn connect!
  [& {:keys [host port scheme verbose]
      :or   {verbose false
             scheme  :tcp}
      :as   opts}]
  (api/connect! :host host :port port :scheme scheme :verbose verbose))

(defn connect-json! [& args]
  (apply api/connect-json! args))
