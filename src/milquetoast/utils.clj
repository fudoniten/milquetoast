(ns milquetoast.utils
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json])
  (:import java.time.Instant))

(defn parallelism []
  (-> (Runtime/getRuntime)
      (.availableProcessors)
      (+ 1)))

(defn pipe [in xf]
  (let [out (async/chan)]
    (async/pipeline (parallelism) out xf in)
    out))

(defn json-parse-message [msg]
  (-> msg
      (update :payload   (fn [payload]
                           (json/read-str payload :key-fn keyword)))
      (assoc  :timestamp (Instant/now))))
