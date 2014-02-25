(defproject wimshurst "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [clj-time "0.6.0"]
                 [endophile "0.1.2"]
                 [enlive "1.1.5"]
                 [ring "1.2.1"]]
  :main ^:skip-aot wimshurst.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
