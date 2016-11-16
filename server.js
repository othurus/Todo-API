var express = require('express');
var bodyParser = require('body-parser');
var _ = require('underscore');
var db = require('./db.js');
var app = express();
var PORT = process.env.PORT || 3000;
var todos = [];
var todoNextId = 1;
app.use(bodyParser.json());
app.get('/', function (req, res) {
    res.send('Todo API Root');
});
// GET /todos
app.get('/todos', function (req, res) {
    var query = req.query;
    var where = {};
    if (query.hasOwnProperty('completed') && query.completed === 'true') where.completed = true;
    else if (query.hasOwnProperty('completed') && query.completed === 'false') where.completed = false;
    if (query.hasOwnProperty('q') && query.q.length > 0) {
        where.description = {
            $like: '%' + query.q + '%'
        }
    }
    db.todo.findAll({
        where: where
    }).then(function (todos) {
        res.json(todos);
    }, function (e) {
        res.status(500).json(e);
    });
    //    var filteredTodos = todos;
    //    if (queryParams.hasOwnProperty('completed') && queryParams.completed === 'true') {
    //        filteredTodos = _.where(filteredTodos, {
    //            completed: true
    //        });
    //    }
    //    else if (queryParams.hasOwnProperty('completed') && queryParams.completed === 'false') {
    //        console.log(_.where(filteredTodos, {
    //            completed: false
    //        }));
    //        filteredTodos = _.where(filteredTodos, {
    //            completed: false
    //        });
    //    }
    //    if (queryParams.hasOwnProperty('q') && queryParams.q.length > 0) {
    //        filteredTodos = _.filter(filteredTodos, function (todo) {
    //            return (todo.description.toLowerCase().indexOf(queryParams.q.toLowerCase()) >= 0);
    //        });
    //    }
    //    res.json(filteredTodos);
});
// GET /todos/:id
app.get('/todos/:id', function (req, res) {
    var todoId = parseInt(req.params.id, 10);
    db.todo.findById(todoId).then(function (todo) {
        if (!!todo) res.json(todo.toJSON());
        else res.status(404).send();
    }, function (e) {
        res.status(500).josn(e);
    });
    //    var matchedTodo = _.findWhere(todos, {
    //        id: todoId
    //    });
    //    if (matchedTodo) {
    //        res.json(matchedTodo);
    //    }
    //    else {
    //        res.status(404).send();
    //    }
});
// POST /todos
app.post('/todos', function (req, res) {
    var body = _.pick(req.body, 'description', 'completed');
    db.todo.create(body).then(function (todo) {
        res.json(todo.toJSON());
    }, function (e) {
        res.status(400).json(e);
    });
    //    if (!_.isBoolean(body.completed) || !_.isString(body.description) || body.description.trim().length === 0) {
    //        return res.status(400).send();
    //    }
    //    body.description = body.description.trim();
    //    body.id = todoNextId++;
    //    todos.push(body);
    //    res.json(body);
});
//POST /users
app.post('/users', function (req, res) {
        var body = _.pick(req.body, 'email', 'password');
        db.user.create(body).then(function (user) {
            res.json(user.toJSON());
        }, function (e) {
            res.status(400).json(e);
        });
    })
    //DELETE /todos/:id
app.delete('/todos/:id', function (req, res) {
    var todoId = parseInt(req.params.id, 10);
    db.todo.destroy({
        where: {
            id: todoId
        }
    }).then(function (rowsDeleted) {
        if (rowsDeleted > 0) res.status(204).send();
        else res.status(404).json({
            "error": "No todo item found with that id!"
        });
    }, function (e) {
        res.status(500).send();
    });
    //    var matchedTodo = _.findWhere(todos, {
    //        id: todoId
    //    });
    //    if (!matchedTodo) {
    //        res.status(400).json({
    //            "error": "no todo item found with that id"
    //        });
    //    }
    //    else {
    //        todos = _.without(todos, matchedTodo);
    //        res.json(todos);
    //    }
});
//PUT /todos/:id
app.put('/todos/:id', function (req, res) {
    var todoId = parseInt(req.params.id, 10);
    var body = _.pick(req.body, 'description', 'completed');
    var attributes = {};
    if (body.hasOwnProperty('completed')) attributes.completed = body.completed;
    if (body.hasOwnProperty('description')) attributes.description = body.description;
    db.todo.findById(todoId).then(function (todo) {
        if (todo) {
            todo.update(attributes).then(function (todo) {
                res.json(todo.toJSON());
            }, function (e) {
                res.status(400).json(e);
            });
        }
        else return res.status(404).json({
            "Error": "No todo with specified id!"
        });
    }, function () {
        res.status(500).send();
    })
});
db.sequelize.sync().then(function () {
    app.listen(PORT, function () {
        console.log('Express listening on port ' + PORT + '!');
    });
});