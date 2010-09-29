;; this file will be loaded and executed by lein
;; clojure code can go here.

;; swank-clojure-project depends on the active buffer, running the command
;; with user.clj active will set (pwd) correctly.
;; (use 'clojure.contrib.duck-streams)
;; (pwd) should return "path-to/project/" and not "path-to/project/src/project"

(comment
  (use 'clojure.contrib.duck-streams)
  (pwd)
  (load-file "script/run.clj")
  )
(println "user.clj has been activated")