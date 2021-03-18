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
