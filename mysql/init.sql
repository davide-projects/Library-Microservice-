CREATE DATABASE IF NOT EXISTS book_service_db;
CREATE DATABASE IF NOT EXISTS member_service_db;
CREATE DATABASE IF NOT EXISTS loan_service_db;

GRANT ALL PRIVILEGES ON book_service_db.* TO 'librarian'@'%';
GRANT ALL PRIVILEGES ON member_service_db.* TO 'librarian'@'%';
GRANT ALL PRIVILEGES ON loan_service_db.* TO 'librarian'@'%';
FLUSH PRIVILEGES;
