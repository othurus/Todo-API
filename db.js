var Sequelize = require('sequelize');
//instance of Sequelize
var sequelize = new Sequelize(undefined, undefined, undefined, {
    'dialect': 'sqlite', // which db to use
    'storage': __dirname + '/data/dev-todo-api.sqlite' //specific to sqlite
});
var db = {};
db.todo = sequelize.import(__dirname + '/models/todo.js');
db.sequelize = sequelize;
db.Sequelize = Sequelize;
module.exports = db;