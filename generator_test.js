function* foo() {
  yield 1
  yield 2
  yield 3
}
// foo()로 생성된 제너레이터를 순회하며 값을 읽어간다.
for (let i of foo()) { 
  console.log(i)
}
// its result are
// 1
// 2
// 3
// almost simultaneously print out maybe 0.1s


//what about this?
function* foo() {
  console.log(yield)
  console.log(yield)
  console.log(yield)
}
let g = foo()
g.next() // start generator
g.next(1)
g.next(2)
g.next(3)

// 이 경우.. 각각의 next는 { value: undefined, done: false or true } 를 리턴하고 있다.. 인자를 넣으면 리턴 값이 아..? 이거 콜솔 로그찍는거구나. 콘솔의 결과는 undefined가 맞지요
// next 에 들어간 인자는 yield 의 값으로 들어가는거군.

function* foo()
{
	let hi = yield 5;
	let hii = yield 6
	let hiii = yield 7
	console.log("result", hi, hii, hiii);	
}
g.next(1000) // { value: 5, done: false }
g.next() // { value: 6, done: false }
g.next(5000) // { value: 7, done : false }
g.next() // {value: undefined, done : true } and > "result undefined 5000 undefined

//결과를 보면 무조건 1번째 호출은 시작을 알리며 첫번째 yield를 호출함을 알 수 있다. 단 이때 1000이 그 값을 대치하지 않는다.
//다음 호출에서 yield 5 위치의 값이 대체되며 이 때 hi 에 undefined가 들어간다.
//연이은 호출에서 hii 에 5000이 hiii 에 undefined 가 할당되고 모든 호출이 끝남으로써 콘솔 찍는 부분도 실행된다.
//즉, next()호출으로 나오는 json 결과는 next 호출 시점보다 1개 빠르다.
