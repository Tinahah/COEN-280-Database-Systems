- COEN-280-Database-Systems Course Project
<b>Background</b>
This is a course project to develop a data analysis application for Yelp.com’s business review data. The emphasis is on the database infrastructure of the application.
In 2013, Yelp.com has announced the “Yelp Dataset Challenge” and invited students to use this data in an innovative way and break ground in research. In this project you would query this dataset to extract useful information for local businesses and individual users.
The Yelp data is available in JSON format. The original Yelp dataset includes 42,153 businesses, 252,898 users, and 1,125,458 reviews from Phoenix (AZ), Las Vegas (NV), Madison (WI) in United States and Waterloo (ON) and Edinburgh (ON) in Canada. (http://www.yelp.com/dataset_challenge/). 
In this course project we use a smaller and simplified dataset. This simplified dataset includes only 20,544 businesses, the reviews that are written for those businesses only, and the users that wrote those reviews. Note: only 10 records of yelp_user.json and yelp_review.json were uploaded because the source file is too large to upload.

<b>.sql files:</b>
1. createdb.sql: This file creates all required tables. In addition, it should include constraints, indexes, and any other DDL statements you might need for your application.
2. dropdb.sql: This file should drop all tables and the other objects once created by your createdb.sql file.

<b>Java programs:</b>
1. populate.java: This program will get the names of the input files as command line parameters and populate them into your database. It should be executed as:
  > java populate yelp_business.json yelp_review.json yelp_checkin.json yelp_user.json
2. hw3.java: This program will provide a GUI, similar to figure 1, to query your database. The GUI includes:
  a. List of main business categories.
  b. List of sub-categories associated with the selected main category(ies).
  c. List of the attributes associated with the selected sub-categories.
  d. 3 dropdown menus to filter results based on days and hours the business is open.
  e. List of business results
    i. Results should include business id, address, city, state, stars, number of reviews, number of check ins.
    ii. List of the reviews provided for a specific business
