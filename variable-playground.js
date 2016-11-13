var person = {
    name: 'Faisal',
    age: 21
}

function updatePerson(obj){
//    //Won't work
//    obj = {
//        name: 'Faisal',
//        age: 23
//    };
    obj.age = 23;
}

updatePerson(person);
console.log(person);

var grades = [87, 100];

function addGrade(arr){
    //arr = [87, 100, 99];
    arr.push(99);
}

addGrade(grades);
console.log(grades);

    