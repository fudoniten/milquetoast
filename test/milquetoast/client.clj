(ns milquetoast.client
  (:require [milquetoast.client :as sut]
            [clojure.test :as t]
            [org.eclipse.paho.client.mqttv3 MqttClient]))

(t/deftest test-create-mqtt-client!
  (t/testing "create-mqtt-client! returns an instance of MqttClient"
    (t/is (instance? MqttClient (sut/create-mqtt-client! :broker-uri "tcp://localhost:1883")))))
