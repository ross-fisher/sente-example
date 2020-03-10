(defproject example "0"
  :description "Example project using sente for clojure."
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [reagent "0.9.1"]
                 [hiccup "2.0.0-alpha2"]
                 [bidi "RELEASE"]
                 [ring "RELEASE" :exclusion [org.clojure/java.classpath]]
                 [thheller/shadow-cljs "RELEASE"]
                 [com.taoensso/sente "1.15.0"]
                 [http-kit "2.3.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.0"]
                 [clojure.joda-time "0.7.0"]
                 [clj-time "0.15.2"]
                 [org.clojure/tools.trace "0.7.10"]
                 ]
  :main example.server
  :source-paths ["src" "src-cljs"]
  :target-path "target/%s"
  :profiles {:dev {:source-paths ["src-dev"]}
             :uberjar {:aot :all}})
                 ; [com.cognitect/transit-clj "1.0.324"]
                 ; [com.cognitect/transit-cljs "0.8.256"]

