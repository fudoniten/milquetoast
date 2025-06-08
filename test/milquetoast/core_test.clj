(ns milquetoast.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [milquetoast.core :as core :refer [MilquetoastClient MilquetoastJsonClient]]))

(deftest test-create-client
  (testing "create-client"
    (let [client (core/create-client "tcp://localhost:1883" "username" "password")]
      (is (instance? MilquetoastClient client)))))

(deftest test-create-json-client
  (testing "create-json-client"
    (let [client (core/create-json-client "tcp://localhost:1883" "username" "password")]
      (is (instance? MilquetoastJsonClient client)))))
