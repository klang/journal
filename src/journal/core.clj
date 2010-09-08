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
   [:body [:h1 title] (str "session: " session) body]))

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

(defn login-controller [session params]
  {:body
   (html "hello there " (params "name") 
	 " " (= "secret" (params "password"))
	 " " (.matches (params "name") "[\\w\\s\\-]+")
	 #_(dosync (alter session assoc :name (params "name")))
	 "session set to " (params :name))
   :session {:name (params "name")}})

(defn password-ok? [params]
  (.matches "secret" (:params "name")))

(comment
  (defn login-controller [session params]
    (if (password-ok?) )))


(defn set-session [session params] {:session {:name (params "name")}})
(defn unset-session [session] {:session {:name nil}})

;; we are going to end up sending a lot of requests to the main page
(defn go-home [] (redirect "/articles/"))

(defn logout-controller [session]
    (dosync
     (alter session assoc :name nil)
     (go-home)))

;; this works, but it butt ugly ..
;; it's not supposed to be written like this, is it?
;; .. session :name is set to parameter input regardless of the password, by the way.


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
  (GET "/articles/" {session :session}
       (view-article-list session))
  (GET "/articles/:title" {session :session, params :params}
	 (view-article session (params "title")))
  (GET "/login/" {session :session}
       (login-view session))
  (POST "/login/" {session :session, params :params}
	  (login-controller session params))
  (ANY "/logout/" {session :session}
       (logout-controller session))
  (ANY "/*" [path] (redirect "/articles/")))

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


(comment
  (defn login-controller [session params]
    (dosync
     (if
	 (and
	  (= "secret" (params "password"))
	  ;; Username can include letters, numbers,
	  ;; spaces, underscores, and hyphens.
	  (.matches (params "name") "[\\w\\s\\-]+"))
       (do
	 (alter session assoc :name (params "name"))
	 (go-home))
       (redirect "/login/"))))

  (defn logout-controller [session]
    (dosync
     (alter session assoc :name nil)
     (go-home)))

  (defroutes handler
    (GET "/articles/" {session :session}
	 (view-article-list session))
    (GET "/articles/:title" {session :session, params :params}
	 (view-article session (params :title)))
    (GET "/login/" {session :session}
	 (login-view session))
    (POST "/login/" {session :session, params :params}
	  (login-controller session params))
    (ANY "/logout/" {session :session}
	 (logout-controller session))
    (ANY "/*" [path] (redirect "/articles"))
    )
)
