# Hotel room reservation service

To run the application, you need to have MySQL server installed and created a schema called finanteq, 
with user finanteq:finanteq to access the database (or change the properties to your discretion).

Once you satisfy the prerequisites, simply run `mvn spring-boot:run` in your console

All API informations are handled by swagger, in default case: `localhost:8080/swagger-ui.html`

The API methods for test scenario are:

- GET /rooms/available/true
- POST /reservations/make
- DELETE /reservations/{reservationId}/cancel

The POST method takes a request body of Reservation DTO:

`{
"startDate": "2017-11-28",
"endDate": "2017-12-30",
"roomId": 8,
"mail": "your@mail.here"
}`

which will return a Reservation object with reservationId which can be used to cancel the
reservation.
There are also methods for getting all rooms and reservations, and to edit them to your needs.

The scheduled job sends an email to reservations a day before they start but can also be 
manually run by running:

- GET /reservations/mail