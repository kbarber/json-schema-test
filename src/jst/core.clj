(ns jst.core
  (:import [com.github.fge.jsonschema.main JsonValidator JsonSchemaFactory]
           [com.github.fge.jsonschema.processors.syntax SyntaxValidator]
           [com.github.fge.jsonschema.report ProcessingReport]
           [com.github.fge.jackson JsonLoader]
           [com.fasterxml.jackson.core JsonFactory]
           [com.fasterxml.jackson.databind JsonNode ObjectMapper]
           [java.io StringWriter])
  (:require [cheshire.core :as json]
            [cheshire.factory :as factory]
            [clojure.pprint]
            [clojure.string :as string]))

(def mapper (ObjectMapper.))

(defn ^JsonNode clojure->jsonnode [x]
  (JsonLoader/fromString (json/generate-string x))) ; any better way of doing this?

(defn ^JsonValidator make-validator-object []
  (.getValidator (JsonSchemaFactory/byDefault)))

(defn ^SyntaxValidator make-syntax-validator-object []
  (.getSyntaxValidator (JsonSchemaFactory/byDefault)))

(defn make-validator [schema]
  (let [v (make-validator-object)]
    (fn [x]
      (.validate v (clojure->jsonnode schema) (clojure->jsonnode x)))))

(defn schema-validate [schema]
  (.validateSchema (make-syntax-validator-object) (clojure->jsonnode schema)))

(defn is-success? [^ProcessingReport report]
  (.isSuccess report))

(defn to-clojure [^ProcessingReport report]
  (let [sw (StringWriter.)
        jgen (.createJsonGenerator (or factory/*json-factory* factory/json-factory) sw)]
    (.writeTree ^ObjectMapper mapper jgen (.asJson report))
    (json/parse-string (.toString sw) true)))

(defn format-result-message [message]
  (let [instance (:instance message)
        pointer  (:pointer instance)
        msg      (:message message)
        expected (string/join "," (:expected message))
        found    (:found message)
        kw       (:keyword message)
        required (string/join "," (:required message))
        missing  (string/join "," (:missing message))]
    (case kw
      "required" (str msg " at pointer '" pointer "' missing '" missing "'")
      "type" (str msg " at pointer '" pointer "', expected '" expected "' but found '" found "'")
      (str "error of type " kw " " msg))))

(defn format-result [^ProcessingReport result]
  (let [result (to-clojure result)]
    (map format-result-message result)))

(defn -main
  [& args]
  (let [schema (json/parse-string (slurp "schema.json"))
        result (schema-validate schema)]
    (if (is-success? result)
      (let [data      (json/parse-string (slurp "data.json"))
            validator (make-validator schema)
            result    (validator data)]
        (if (is-success? result)
          nil
          (do
            (println (to-clojure result))
            (println (str "Problem validating configuration:\n  " (string/join "\n  " (format-result result)))))))
      (do
        (println "Invalid schema")
        (clojure.pprint/pprint (to-clojure result))))))
