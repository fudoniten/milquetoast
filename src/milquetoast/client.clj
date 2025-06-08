(ns milquetoast.client
  (:require [milquetoast.api :as api]))

(defn send! [client topic msg & {:keys [qos retain]
                                 :or   {qos 1 retain false}}]
  ...)

(defn get! [client topic & options]
  ...)

(defn get-raw! [client topic & options]
  ...)

(defn open-channel!
  [client topic & {:keys [buffer-size qos retain]
                   :or   {buffer-size 1
                          qos         1
                          retain      false}}]
  ...)

(defn subscribe!
  [client topic & {:keys [buffer-size qos]
                   :or   {buffer-size 1
                          qos         1}}]
  ...)

(defn connect!
  [& {:keys [host port scheme verbose]
      :or   {verbose false
             scheme  :tcp}
      :as   opts}]
  ...)

(defn connect-json! [& args]
  ...)
