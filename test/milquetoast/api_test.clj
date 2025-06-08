(ns milquetoast.api-test
  (:require [milquetoast.api :as sut]
            [clojure.test :as t]
            [clojure.core.async :as async]))

(t/deftest test-send!
  (t/testing "send! function"
    (let [client (sut/connect! :host "localhost" :port 1883)
          topic "test/topic"
          msg "Hello, World!"]
      (sut/send! client topic msg)
      ;; Add more assertions here
      )))

(t/deftest test-get!
  (t/testing "get! function"
    (let [client (sut/connect! :host "localhost" :port 1883)
          topic "test/topic"]
      (sut/get! client topic)
      ;; Add more assertions here
      )))

(t/deftest test-get-raw!
  (t/testing "get-raw! function"
    (let [client (sut/connect! :host "localhost" :port 1883)
          topic "test/topic"]
      (sut/get-raw! client topic)
      ;; Add more assertions here
      )))

(t/deftest test-open-channel!
  (t/testing "open-channel! function"
    (let [client (sut/connect! :host "localhost" :port 1883)
          topic "test/topic"]
      (sut/open-channel! client topic)
      ;; Add more assertions here
      )))

(t/deftest test-subscribe!
  (t/testing "subscribe! function"
    (let [client (sut/connect! :host "localhost" :port 1883)
          topic "test/topic"]
      (sut/subscribe! client topic)
      ;; Add more assertions here
      )))

(t/deftest test-connect!
  (t/testing "connect! function"
    (let [client (sut/connect! :host "localhost" :port 1883)]
      ;; Add assertions here
      )))

(t/deftest test-connect-json!
  (t/testing "connect-json! function"
    (let [client (sut/connect-json! :host "localhost" :port 1883)]
      ;; Add assertions here
      )))
