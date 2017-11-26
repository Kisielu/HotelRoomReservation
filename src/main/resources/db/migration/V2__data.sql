INSERT INTO room (is_reserved) VALUES (0), (0), (0), (0), (0), (1), (0), (1);

INSERT INTO reservation (reservation_start_date, reservation_end_date, room_id)
VALUES ('2017-11-28', '2017-11-30', 6), ('2017-11-01', '2017-11-10', 6), ('2017-11-21', '2017-11-29', 8), ('2017-11-11', '2017-11-20', 8);