(ns milquetoast.core-test
  (:require [clojure.test :refer :all]
            [milquetoast.core :as core]))

(deftest test-create-client
  (testing "create-client"
    (let [client (core/create-client "tcp://localhost:1883" "username" "password")]
      (is (instance? core.MilquetoastClient client)))))

(deftest test-create-json-client
  (testing "create-json-client"
    (let [client (core/create-json-client "tcp://localhost:1883" "username" "password")]
      (is (instance? core.MilquetoastJsonClient client)))))

(deftest test-send-message!
  (testing "send-message!"
    ;; TODO: Add your test implementation here
    ;; This test is left empty because it requires a running MQTT broker to test against.
    ))

(deftest test-stop!
  (testing "stop!"
    ;; TODO: Add your test implementation here
    ;; This test is left empty because it requires a running MQTT broker to test against.
    ))

(deftest test-add-channel!
  (testing "add-channel!"
    ;; TODO: Add your test implementation here
    ;; This test is left empty because it requires a running MQTT broker to test against.
    ))

(deftest test-subscribe-topic!
  (testing "subscribe-topic!"
    ;; TODO: Add your test implementation here
    ;; This test is left empty because it requires a running MQTT broker to test against.
    ))

(deftest test-get-topic!
  (testing "get-topic!"
    ;; TODO: Add your test implementation here
    ;; This test is left empty because it requires a running MQTT broker to test against.
    ))

(deftest test-get-topic-raw!
  (testing "get-topic-raw!"
    ;; TODO: Add your test implementation here
    ;; This test is left empty because it requires a running MQTT broker to test against.
    ))
