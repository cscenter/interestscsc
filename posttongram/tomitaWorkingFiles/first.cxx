#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики
#GRAMMAR_KWSET ["плохие_словосочетания"]
S -> Word<gram='A'> interp (NGrams.Unigram::norm="nom,pl,brev,m");
//S -> Word<gram='V,brev'> interp (Word.Norm);
//comp
//S -> Word<GU=['A,comp']> interp (Word.Norm);
S -> Word<gram='ger',no_hom> interp (NGrams.Unigram::norm="inf"); //? <no_hom>
S -> Noun<GU=~['abbr']> interp (NGrams.Unigram::norm="nom,sg");
//S -> Word<GU=&['S','ADV']> | Word<GU=&['S','V']> interp (Word.Norm::norm="nom,sg");
S -> Verb interp (NGrams.Unigram::norm="inf");
S -> Word<gram='imper'> interp (NGrams.Unigram::norm="inf");
S -> Word<gram='inf'> interp (NGrams.Unigram);
S -> Participle interp (NGrams.Unigram::norm="m,sg,nom,plen");
S -> Word<gram='pass'> interp (NGrams.Unigram::norm="m,brev");
//S -> Word<gram='ADV',GU=~['comp']> interp (Word.Norm);
S -> Word<gram='ADV',no_hom,GU=~['comp']> interp (NGrams.Unigram);
