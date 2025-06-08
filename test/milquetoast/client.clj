(ns milquetoast.client
  (:require [milquetoast.client :as sut]
            [clojure.test :as t]
            [org.eclipse.paho.client.mqttv3 MqttClient]))

(t/deftest test-create-mqtt-client!
  (t/testing "create-mqtt-client! returns an instance of MqttClient"
    (t/is (instance? MqttClient (sut/create-mqtt-client! :broker-uri "tcp://localhost:1883")))))

(t/deftest test-retry-attempt
  (t/testing "retry-attempt retries function execution upon RuntimeException"
    (let [attempt-count (atom 0)]
      (sut/retry-attempt false
                         (fn []
                           (swap! attempt-count inc)
                           (when (< @attempt-count 3)
                             (throw (RuntimeException. "Test exception"))))
                         #(println "Reconnecting..."))
      (t/is (= 3 @attempt-count)))))

(t/deftest test-create-message
  (t/testing "create-message creates an MQTT message with the provided options"
    (let [msg (sut/create-message "test" {:qos 2 :retain true})]
      (t/is (= 2 (.getQos msg)))
      (t/is (.isRetained msg)))))

(t/deftest test-parse-message
  (t/testing "parse-message parses an MQTT message into a map"
    (let [mqtt-msg (sut/create-message "test" {:qos 2 :retain true})
          parsed-msg (sut/parse-message mqtt-msg)]
      (t/is (= 2 (:qos parsed-msg)))
      (t/is (:retained parsed-msg)))))

(t/deftest test-parallelism
  (t/testing "parallelism returns the number of available processors plus one"
    (t/is (= (inc (.availableProcessors (Runtime/getRuntime))) (sut/parallelism)))))

(t/deftest test-json-parse-message
  (t/testing "json-parse-message parses the payload of an MQTT message into a map and adds a timestamp"
    (let [mqtt-msg (sut/create-message "{\"test\": \"value\"}" {:qos 2 :retain true})
          parsed-msg (sut/json-parse-message (sut/parse-message mqtt-msg))]
      (t/is (= "value" (:test (:payload parsed-msg))))
      (t/is (instance? java.time.Instant (:timestamp parsed-msg))))))
