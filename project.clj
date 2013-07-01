(def version-string 
  (memoize 
    (fn [] "0.1.0")))

(defproject jst (version-string)
  :description "JSON Schema Test"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.github.fge/json-schema-validator "2.0.1"]
                 [com.github.fge/jackson-coreutils "1.4"]
                 [cheshire "5.2.0"]]

  :jar-exclusions [#"leiningen/"]

  :aot [jst.core]
  :main jst.core
)
