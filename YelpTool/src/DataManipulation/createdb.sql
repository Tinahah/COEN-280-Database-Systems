/* create table yelp_business_mainCatg to store business mainCategory info */
create table yelp_business_mainCatg (
    name varchar(30) primary key
);

/* create table yelp_business to store business info */
create table yelp_business (
    businessID varchar(30) primary key,
    address varchar(200),
    open varchar(5),
    city varchar(30),
    state varchar(5),
    latitude number,
    longitude number,
    reviewCount number,
    name varchar(100),
    neighborhoods varchar(500),
    stars number check(stars between 1 and 5)
);

/* create table yelp_businessSubCategory to store business subCategory info */
create table yelp_businessCategory (
    businessID varchar(30) constraint businessFk1 references yelp_business(businessID) on delete cascade,
    mainCategory varchar(100),
    subCategory varchar(100),
    primary key(businessID, mainCategory, subCategory)
);

/* create table yelp_businessHours to store business hours info */
create table yelp_businessHours (
    businessID varchar(30) constraint businessFk2 references yelp_business(businessID) on delete cascade,
    days varchar(10),
    closeTime varchar(5),
    openTime varchar(5),
    primary key(businessID, days)
);

/* create table yelp_businessAttribute to store business attribute info */
create table yelp_businessAttribute (
    businessID varchar(30) constraint businessFk3 references yelp_business(businessID) on delete cascade,
    attrNameValue varchar(500),
    primary key(businessID, attrNameValue)
);

/* create table yelp_businessCheckin to store business checkin info */
create table yelp_businessCheckin (
    businessID varchar(30) constraint businessFk4 references yelp_business(businessID) on delete set null,
    hourDay varchar(4),
    num number,
    primary key(businessID, hourDay)
);

/* create table yelp_yelpUser to store user info */
create table yelp_yelpUser (
    yelpID varchar(30) primary key,
    yelpSince varchar(7),
    votesFunny number,
    votesCool number,
    votesUseful number,
    reviewCount number,
    name varchar(50),
    averageStars number check(averageStars between 0 and 5),
    friends clob,
    fans number,
    elite varchar(200)
);

/* create table yelp_yelpUserCompliment to store user compliment info */
create table yelp_yelpUserCompliment (
    yelpID varchar(30) constraint userFk references yelp_yelpUser(yelpID) on delete cascade,
    compName varchar(20),
    compValue number,
    primary key(yelpID, compName)
);

/* create table yelp_review to store review info */
create table yelp_review (
    reviewID varchar(30) primary key,
    votesFunny number,
    votesUseful number,	
    votesCool number,
    stars number check(stars between 0 and 5),
    rdate varchar(10),
    text clob,
    user_id varchar(30) constraint authorFk references yelp_yelpUser(yelpID) on delete cascade,
    businessID varchar(30) constraint businessFk5 references yelp_business(businessID) on delete cascade
);

/* create view business_info to combine business and checkin data. */
create or replace view v_yelp_business_info as
select name, b.businessID, address, city, state, stars, reviewCount, c.checkCount
from yelp_business b
left join
    (
        select businessid, sum(num) as checkcount 
        from yelp_businessCheckin 
        group by businessID
    ) c
on b.businessID = c.businessID;