isNullOrUndef = (value) => value === null || typeof value === "undefined";

idf = v => v;

isfunc = (v) => typeof v === 'function'

pure = v => Maybe.just(v)

maybe = (value) => ({
    isNothing: () => isNullOrUndef(value),
    extract: () => value,
    map: (transformer) => {
           if (isNullOrUndef(value))
                return Maybe.nothing();
           if (isfunc(value))
                return Maybe.just(x => transformer(value(x)));
           return Maybe.just(transformer(value));
    },
        ap: (func) => {
           if (func.isNothing() || isNullOrUndef(value))
                return Maybe.nothing();
           if (isfunc(value) && isfunc(func.extract()))
           {
                return Maybe.just(x => func.extract()(value(x)));
           }
           return isfunc(func.extract()) ? Maybe.just(func.extract()(value)) : func.map(value)
    },
    bind: (func) => {
        if (isNullOrUndef(value))
            return Maybe.nothing();
        return func(value);
    }
});

Maybe = {
    just: maybe,
    nothing: () => maybe(null)
};

——————

bi = Maybe.just(50)
 .bind( x => Maybe.just(100)
    .bind(y => Maybe.just(200 + x)
        .bind(z => Maybe.just(x + y + z))))

function Do(m, f) {
  const gen = f();

  function doRec(v = undefined) {
    const {value, done} = gen.next(v);
    //const valueM = value instanceof m ? value : m.of(value);
    //return done ? valueM : valueM.bind(doRec);
    return done ? value : value.bind(doRec);
  }

  return doRec();
} // code from https://gist.github.com/MaiaVictor/bc0c02b6d1fbc7e3dbae838fb1376c8

// and can I apply this mechanism to those binds chain?0

function trys(maybeValue)
{
	return Do(Maybe, function*()
	{
		let first = yield (x => Maybe.just(100))
		let second = yield (y => Maybe.just(200 * first))
		return (z => Maybe.just(first + second + z))
	})
}
//뭔가 이상한데.. first를 더하고자 하는게 아닌데...
function trys(maybeValue)
{
	return Do(Maybe, function*()
	{
		let x = yield maybeValue
		let y = yield Maybe.just(100)
		let z = yield Maybe.just(200 + x)
		return Maybe.just(x + y + z)
	})
}


//이렇게 하면..

bi = trys(pure(50)) // bi.extract() -> 400
