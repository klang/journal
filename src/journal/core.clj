(ns journal.core
  (:use compojure.core
	hiccup.core
	hiccup.page-helpers
	hiccup.form-helpers
	journal.middleware
	journal.database
	ring.handler.dump
	ring.middleware.session
	ring.middleware.file
	ring.middleware.file-info
	ring.middleware.reload
	ring.middleware.stacktrace
	ring.util.response)
  (:require [compojure.route :as route]))

(def production?
  (= "production" (get (System/getenv) "APP_ENV")))

(def development?
  (not production?))

(defn page [session title body]
  (html
   [:head [:title title]]
   [:body [:h1 title] body
    [:p
     (if (session :name)
       (link-to "/logout/"
		(str "Log out " (session :name)))
       (link-to "/login/" "Log in"))]]))

(defn article-title-to-url-name [title]
  (.replaceAll
    (.toLowerCase title)
    "[^a-z0-9]+" "-"))

(defn article-url [article]
  (str
    "/articles/"
    (article-title-to-url-name
      (article :title))))

;; TODO: improve this
(defn fetch-articles []
  (into [] (select-articles)))

;; TODO: improve this
(defn fetch-article [title]
  (first
    (filter
      (fn [art]
        (=
          (article-title-to-url-name
            title)
          (article-title-to-url-name
            (art :title))))
      (fetch-articles))))

(defn render-article [article]
  [:div 
   [:p [:em (article :description)]] 
   (article :body)])

(defn view-article [session title]
  (try
    (let [article (fetch-article title)]
      (page session (article :title)
	    (render-article article)))
    (catch Exception ex
      (str ex) #_(redirect "/articles/"))))


(defn render-article-link [article]
  (link-to
    (article-url article)
    (article :title)))

(defn view-article-list [session]
  (page session "Articles"
	[:dl (mapcat
	      (fn [article]
		(list
		 [:dt (render-article-link
		       article)]
		 [:dd (article
		       :description)]))
	      (fetch-articles))]))

(defn login-view [session]
  (page session "Log in"
	[:form {:method "post"}
	 "User name: "
	 [:input {:name "name", :type "text"}]
	 [:br]
	 "Password: "
	 [:input {:name "password", :type "password"}]
	 [:br]
	 [:input {:type "submit" :value "Log in"}]]))

;; we are going to end up sending a lot of requests to the main page
(defn go-home [] (redirect "/articles/"))

(defn password-ok? [params]
  (= "secret" (params "password")))

(defn login-controller [session params]
  (if (password-ok? params)
    {:body (html "access granted, " (params "name") 
		 "<br> back to " (link-to "/" "main"))
     :session {:name (params "name")}}
    {:body (html "access not granted, " (params "name")  
		 "<br> try again: " (link-to "/login/" "login"))
     :session {:name nil}}))

;; this works, but it butt ugly ..
;; it's not supposed to be written like this, is it?

(defn logout-controller [session]
  {:body
   (html "bye for now"
	 "<br> back to " (link-to "/" "main"))
   :session {:name nil}})

(defn parse-input [a b]
  [(Integer/parseInt a) (Integer/parseInt b)])

(defroutes handler
  ;; ----- experiments -----
  ;; regexp check directly in link
  (GET ["/user/:id", :id #"[0-9]+"] [id] (str "userid=" id))
  (GET ["/user/:name", :id #"[a-z]+"] [name] (str "username=" name))
  ;; simpel example to illustrate the use of sessions
  (GET "/set-session" [] {:body "set session " :session {:a-key "a value" :b-key "b key"}})
  (GET "/read-session" {s :session} {:body (str "session: " s)})
  (GET "/unset-value" {s :session} {:body "changing session" :session (dissoc s :b-key)})
  ;; different experiments with parameters
  (GET "/foo*" request {:body (str ((request :params) :user) "\n" request)}) 
  (GET "/bar" request {:body (str ((request :params) :user) "\n" request)}) 
  (GET "/barf" {params :params} {:body (str (params :user) "\nparams:" params)}) 
  (GET "/barfu/:user" [user] {:body (str user)})
  ;; -----------------------
  ;; the application
  (GET  "/articles/" {session :session}
	(view-article-list session))
  (GET  "/articles/:title" {session :session, params :params}
	(view-article session (params "title")))
  (GET  "/login/" {session :session}
	(login-view session))
  (POST "/login/" {session :session, params :params}
	(login-controller session params))
  (ANY  "/logout/" {session :session}
	(logout-controller session))
  (ANY  "/*" [path] (redirect "/articles/")))

(def app (-> #'handler
	     (wrap-utf)
	     (wrap-file "public")
	     (wrap-file-info)
	     (wrap-request-logging)
	     (wrap-session)
	     ;; reloading the app just before a request is made is pretty bad for sessions (they die)
	     #_(wrap-if development? wrap-reload '[journal.middleware journal.core])
	     (wrap-bounce-favicon)
	     (wrap-exception-logging)
	     (wrap-if production?  wrap-failsafe)
	     (wrap-if development? wrap-stacktrace)))


