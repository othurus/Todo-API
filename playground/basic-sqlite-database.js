var Sequelize = require('sequelize');
//instance of Sequelize
var sequelize = new Sequelize(undefined, undefined, undefined, {
    'dialect': 'sqlite', // which db to use
    'storage': __dirname + '/basic-sqlite-database.sqlite' //specific to sqlite
});
//create a model (table/entity)
var Todo = sequelize.define('todo', {
    description: {
        type: Sequelize.STRING
        , allowNull: false
        , validate: {
            len: [1, 250]
        }
    }
    , completed: {
        type: Sequelize.BOOLEAN
        , allowNull: false
        , defaultValue: false
    }
});
//force = true -> create table even if exist (default: false -> only create if not exist)
sequelize.sync({
    //force: true
}).then(function () { //sync is a promise
    console.log('Everything is synced');
    Todo.findOne({
        where: {
            id: 1
        }
    }).then(function (todo) {
        if (todo) console.log(todo.toJSON());
        else console.log('unable to find todo item with the specified id');
    });
    //    Todo.create({
    //        description: 'Walk the dog'
    //    }).then(function () {
    //        return Todo.create({
    //            description: 'solve exam'
    //        });
    //    }).then(function () {
    //        //return Todo.findById(1);
    //        return Todo.findAll({ //return arr
    //            where: {
    //                completed: false
    //            }
    //        });
    //    }).then(function (todos) {
    //        console.log('Printing the todo items:')
    //        if (todos) {
    //            todos.forEach(function (todo) {
    //                console.log(todo.toJSON());
    //            });
    //        }
    //        else console.log('no todo found!');
    //    }).catch(function (e) {
    //        console.log(e);
    //    });
});