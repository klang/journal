(ns hello-world.core
  (:use compojure.core
        ring.middleware.session
        ring.adapter.jetty))

(defroutes handler
  (GET "/set-session" []
    {:body "set session"
     :session {:a-key "a value"}})
  (GET "/read-session" {s :session}
       {:body (str "session: " s)})
  (ANY "/*" [] (str "not found")))

(def app (-> #'handler (wrap-session)))

(future (run-jetty app {:port 8090}))
