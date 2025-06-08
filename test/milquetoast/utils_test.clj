(ns milquetoast.utils-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.async :refer [chan >!! <!!]]
            [milquetoast.utils :as utils]))

(deftest test-parallelism
  (testing "parallelism"
    (is (integer? (utils/parallelism)))))

(deftest test-pipe
  (testing "pipe"
    (let [in (chan)
          out (utils/pipe in (map inc))]
      (>!! in 1)
      (is (= 2 (<!! out))))))

(deftest test-json-parse-message
  (testing "json-parse-message"
    (let [msg {:payload "{\"key\":\"value\"}"}
          parsed-msg (utils/json-parse-message msg)]
      (is (= {:key "value"} (:payload parsed-msg))))))
