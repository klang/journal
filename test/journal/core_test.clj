(ns journal.core-test
  #_(:use [journal.core] :reload-all)
  (:use [journal.core])
  #_(:use [journal.database] :reload-all)
  (:use [clojure.test]))

(deftest parse-input-valid
  (is (= [1 2] (parse-input "1" "2"))))

(deftest parse-input-invalid
  (is (thrown? NumberFormatException
    (parse-input "foo" "bar"))))

(deftest handle-articles
  (let [resp (handler {:uri "/articles/" :request-method :get} )]
    (is (= 200 (:status resp)))
    (is (re-find #"Articles" (:body resp)))))

(deftest handle-article-valid
  (let [resp (handler {:uri "/articles/article-1" :request-method :get} )]
    (is (= 200 (:status resp)))
    (is (re-find #"Article 1" (:body resp)))))

(deftest handle-article-invalid
  (let [resp (handler {:uri "/articles/article-0" :request-method :get} )]
    (is (= 302 (:status resp)))
    (is (= "/articles/" (get-in resp [:headers "Location"])))))


(deftest handle-set
  (let [resp (handler {:uri "/set" :request-method :get :params {:user "klang"}} )]
    (is (= 200 (:status resp)))
    (is (= (:user (:session resp) "klang")))))

(deftest handle-get
  (let [resp (handler {:uri "/get" :request-method :get :session {:user "klang"}} )]
    (is (= 200 (:status resp)))
    (is (= (re-find #"Hello klang" (:body resp))))))

(deftest handle-catchall
  (let [resp (handler {:uri "/" :request-method :get} )]
    (is (= 302 (:status resp)))
    (is (= "/articles/" (get-in resp [:headers "Location"])))))

;;(run-tests)