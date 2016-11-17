var Sequelize = require('sequelize');
//instance of Sequelize
var sequelize = new Sequelize(undefined, undefined, undefined, {
    'dialect': 'sqlite', // which db to use
    'storage': __dirname + '/basic-sqlite-database.sqlite' //specific to sqlite
});
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
var User = sequelize.define('user', {
    email: Sequelize.STRING
});
Todo.belongsTo(User);
User.hasMany(Todo);
sequelize.sync({
    force: false
}).then(function () {
    console.log('Everything is synced');
    User.findById(1).then(function (user) {
        user.getTodos({
            where: {
                completed: false
            }
        }).then(function (todos) {
            todos.forEach(function (todo) {
                console.log(todo.toJSON());
            });
        });
    });
    //    User.create({
    //        email: "andrew@gmail.com"
    //    }).then(function () {
    //        return Todo.create({
    //            description: 'Clean the yard'
    //        });
    //    }).then(function (todo) {
    //        User.findById(1).then(function (user) {
    //            user.addTodo(todo);
    //        });
    //    });
});