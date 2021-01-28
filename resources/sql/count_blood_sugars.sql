select value, count(value) as count
from blood_sugars 
group by value
