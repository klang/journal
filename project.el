;; M-x eval-buffer
;; M-x clj-joural
;; M-x swank-clojure-project
;;
;; and to start jetty, in clojure
;; (load-file "script/run.clj")
(if (not (fboundp 'defwrk))
    (defmacro defwrk (name description &rest files)
      `(defun ,name ()
         ,description
         (interactive)
         (let ((files '(,@files)))
           (loop while files collecting (find-file (pop files)))))))

(defun clj-journal nil
  "Start project with some custom setting I have in my global .emacs"
  (interactive)
  (setq project-directory "~/projects")
  (setq project-name "snippet")

  (setq default-directory (concat project-directory "/" project-name "/"))

  (defwrk clj-current "important files in this project"
     "~/projects/journal/project.clj"
     "~/projects/journal/project.el"
     "~/projects/journal/src/journal/core.clj"
     "~/projects/journal/user.clj")
  (clj-current)
  )
