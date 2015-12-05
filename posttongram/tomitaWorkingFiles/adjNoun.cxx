#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики

// + остальные совпадения пола / числа / падежа
SuperAdjmacc -> Word<GU=[A,acc,m]|[V,acc,m]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjmacc+ Noun<GU=&[acc,m],no_hom, rt> interp(NGrams.Head);


SuperAdjmnom -> Adj<GU=&[A,nom,m]|[V,nom,m]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjmnom+ Noun<GU=&[nom,m],no_hom, rt> interp(NGrams.Head);


SuperAdjmgen -> Word<GU=[A,gen,m]|[V,gen,m]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjmgen+ Noun<GU=&[gen,m],no_hom, rt> interp(NGrams.Head);


SuperAdjmdat -> Word<GU=[A,dat,m]|[V,dat,m]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjmdat+ Noun<GU=&[dat,m],no_hom, rt> interp(NGrams.Head);


SuperAdjmins -> Word<GU=[A,ins,m]|[V,ins,m]> interp(NGrams.Dependent) Word*;

S -> SuperAdjmins+ Noun<GU=[ins,m],no_hom, rt> interp(NGrams.Head);



SuperAdjfacc -> Word<GU=[A,acc,f]|[V,acc,f]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjfacc+ Noun<GU=&[acc,f],no_hom, rt> interp(NGrams.Head);


SuperAdjfnom -> Adj<GU=&[A,nom,f]|[V,nom,f]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjfnom+ Noun<GU=&[nom,f],no_hom, rt> interp(NGrams.Head);


SuperAdjfgen -> Word<GU=[A,gen,f]|[V,gen,f]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjfgen+ Noun<GU=&[gen,f],no_hom, rt> interp(NGrams.Head);


SuperAdjfdat -> Word<GU=[A,dat,f]|[V,dat,f]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjfdat+ Noun<GU=&[dat,f],no_hom, rt> interp(NGrams.Head);


SuperAdjfins -> Word<GU=[A,ins,f]|[V,ins,f]> interp(NGrams.Dependent) Word*;

S -> SuperAdjfins+ Noun<GU=[ins,f],no_hom, rt> interp(NGrams.Head);



SuperAdjnacc -> Word<GU=[A,acc,n]|[V,acc,n]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjnacc+ Noun<GU=&[acc,n],no_hom, rt> interp(NGrams.Head);


SuperAdjnnom -> Adj<GU=&[A,nom,n]|[V,nom,n]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjnnom+ Noun<GU=&[nom,n],no_hom, rt> interp(NGrams.Head);


SuperAdjngen -> Word<GU=[A,gen,n]|[V,gen,n]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjngen+ Noun<GU=&[gen,n],no_hom, rt> interp(NGrams.Head);


SuperAdjndat -> Word<GU=[A,dat,n]|[V,dat,n]> interp(NGrams.Dependent) Word<GU=~[S]>*;

S -> SuperAdjndat+ Noun<GU=&[dat,n],no_hom, rt> interp(NGrams.Head);


SuperAdjnins -> Word<GU=[A,ins,n]|[V,ins,n]> interp(NGrams.Dependent) Word*;

S -> SuperAdjnins+ Noun<GU=[ins,n],no_hom, rt> interp(NGrams.Head);

