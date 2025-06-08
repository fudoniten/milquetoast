(ns milquetoast.client
  (:require [milquetoast.api :as api])
  (:import [clojure.lang ExceptionInfo]))

(defn- deprecated [f]
  (fn [& args]
    (println (str "WARNING: The function '" (name f) "' in the 'milquetoast.client' namespace is deprecated. Please use 'milquetoast.api' instead."))
    (apply f args)))

(defmacro defdeprecated [name & body]
  `(def ~name (deprecated (fn ~@body))))

(println "WARNING: The 'milquetoast.client' namespace is deprecated. Please use 'milquetoast.api' instead.")
