(use 'ring.adapter.jetty)
(require 'journal.core)

;;(require 'swank.swank)
;;(swank.swank/start-repl)

(let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
  (future (run-jetty #'journal.core/app {:port port})))
