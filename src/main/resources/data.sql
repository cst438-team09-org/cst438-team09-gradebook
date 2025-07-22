insert into term (term_id, tyear, semester, add_date, add_deadline, drop_deadline, start_date, end_date) values
 (9, 2025, 'Spring', '2024-11-01', '2025-04-30', '2025-04-30', '2025-01-15', '2025-05-17'),
 (10, 2025, 'Fall',  '2025-04-01', '2025-09-30', '2025-09-30', '2025-08-20', '2025-12-17');

insert into user_table (id, name, email, password, type) values
 (1, 'admin', 'admin@csumb.edu', '$2a$10$8cjz47bjbR4Mn8GMg9IZx.vyjhLXR/SKKMSZ9.mP9vpMu0ssKi8GW' , 'ADMIN'),
 (2, 'sam', 'sam@csumb.edu', '$2a$10$jt2znhe0fT6z39Xlgc2DnOTB1QLuaPzGxmzbs0KHn4JDFf.8Gvcmu', 'STUDENT'),
 (3, 'ted', 'ted@csumb.edu', '$2a$10$lvIkD3b9WUcvEeSqVuDmdeDGm0dtkZKLYMZUfs6YBIWGb4.1GP1VS', 'INSTRUCTOR');


insert into course values
('cst336', 'Internet Programming', 4),
('cst334', 'Operating Systems', 4),
('cst363', 'Introduction to Database', 4),
('cst489', 'Software Engineering', 4),
('cst499', 'Capstone', 4);

insert into section (section_no, course_id, section_id, term_id, building, room, times, instructor_email) values
(1, 'cst489', 1, 10, '90', 'B104', 'W F 10-11', 'ted@csumb.edu');

-- insert into enrollment (grade, section_no, user_id) values (null, 1, 2);