(defproject journal "1.0.0-SNAPSHOT"
  :description "Eric Lavigne's Journal Server, updated and converted to use hsqldb"
  :dependencies
  [[org.clojure/clojure "1.2.0"]
   [org.clojure/clojure-contrib "1.2.0"]
   [ring/ring-core "0.2.5"]
   [ring/ring-devel "0.2.5"]
   [ring/ring-servlet "0.2.5"]
   [ring/ring-jetty-adapter "0.2.5"]
   [compojure "0.4.1"]
   [hiccup "0.2.6"]
   [hsqldb "1.8.0.10"]] ;"2.0.0"
  :dev-dependencies
  [[lein-run "1.0.0-SNAPSHOT"]
   [swank-clojure "1.2.1"]]
  ;;:jvm-opts ["-server" " -Xmx64M"]
  )
