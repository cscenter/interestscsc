#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики

SuperAdjfacc -> Word<GU=[A,acc,f]|[V,acc,f]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[acc,f],no_hom, rt> interp(NGrams.Head) SuperAdjfacc+;


SuperAdjfnom -> Adj<GU=&[A,nom,f]|[V,nom,f]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[nom,f],no_hom, rt> interp(NGrams.Head) SuperAdjfnom+;


SuperAdjfgen -> Word<GU=[A,gen,f]|[V,gen,f]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[gen,f],no_hom, rt> interp(NGrams.Head) SuperAdjfgen+;


SuperAdjfdat -> Word<GU=[A,dat,f]|[V,dat,f]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[dat,f],no_hom, rt> interp(NGrams.Head) SuperAdjfdat+;


SuperAdjfins -> Word<GU=[A,ins,f]|[V,ins,f]> interp(NGrams.Dependent::norm="m") Word*;

S -> Noun<GU=&[ins,f],no_hom, rt> interp(NGrams.Head) SuperAdjfins+;


SuperAdjmacc -> Word<GU=[A,acc,m]|[V,acc,m]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[acc,m],no_hom, rt> interp(NGrams.Head) SuperAdjmacc+;


SuperAdjmnom -> Adj<GU=&[A,nom,m]|[V,nom,m]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[nom,m],no_hom, rt> interp(NGrams.Head) SuperAdjmnom+;


SuperAdjmgen -> Word<GU=[A,gen,m]|[V,gen,m]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[gen,m],no_hom, rt> interp(NGrams.Head) SuperAdjmgen+;


SuperAdjmdat -> Word<GU=[A,dat,m]|[V,dat,m]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[dat,m],no_hom, rt> interp(NGrams.Head) SuperAdjmdat+;


SuperAdjmins -> Word<GU=[A,ins,m]|[V,ins,m]> interp(NGrams.Dependent::norm="m") Word*;

S -> Noun<GU=&[ins,m],no_hom, rt> interp(NGrams.Head) SuperAdjmins+;



SuperAdjnacc -> Word<GU=[A,acc,n]|[V,acc,n]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[acc,n],no_hom, rt> interp(NGrams.Head) SuperAdjnacc+;


SuperAdjnnom -> Adj<GU=&[A,nom,n]|[V,nom,n]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[nom,n],no_hom, rt> interp(NGrams.Head) SuperAdjnnom+;


SuperAdjngen -> Word<GU=[A,gen,n]|[V,gen,n]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[gen,n],no_hom, rt> interp(NGrams.Head) SuperAdjngen+;


SuperAdjndat -> Word<GU=[A,dat,n]|[V,dat,n]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[dat,n],no_hom, rt> interp(NGrams.Head) SuperAdjndat+;


SuperAdjnins -> Word<GU=[A,ins,n]|[V,ins,n]> interp(NGrams.Dependent::norm="m") Word*;

S -> Noun<GU=&[ins,n],no_hom, rt> interp(NGrams.Head) SuperAdjnins+;



SuperAdjplacc -> Word<GU=[A,acc,pl]|[V,acc,pl]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[acc,pl],no_hom, rt> interp(NGrams.Head) SuperAdjplacc+;


SuperAdjplnom -> Adj<GU=&[A,nom,pl]|[V,nom,pl]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[nom,pl],no_hom, rt> interp(NGrams.Head) SuperAdjplnom+;


SuperAdjplgen -> Word<GU=[A,gen,pl]|[V,gen,pl]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[gen,pl],no_hom, rt> interp(NGrams.Head) SuperAdjplgen+;


SuperAdjpldat -> Word<GU=[A,dat,pl]|[V,dat,pl]> interp(NGrams.Dependent::norm="m") Word<GU=~[S]>*;

S -> Noun<GU=&[dat,pl],no_hom, rt> interp(NGrams.Head) SuperAdjpldat+;


SuperAdjplins -> Word<GU=[A,ins,pl]|[V,ins,pl]> interp(NGrams.Dependent::norm="m") Word*;

S -> Noun<GU=&[ins,pl],no_hom, rt> interp(NGrams.Head) SuperAdjplins+;
