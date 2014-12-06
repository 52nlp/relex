/*
 * Copyright 2009 Linas Vepstas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package relex.test;

import java.util.ArrayList;
import java.util.Collections;

import relex.ParsedSentence;
import relex.RelationExtractor;
import relex.Sentence;
import relex.output.SimpleView;

public class TestRelEx
{
	private static RelationExtractor re;
	private int pass;
	private int fail;
	private int subpass;
	private int subfail;
	private static ArrayList<String> sentfail= new ArrayList<String>();

	// @BeforeClass
	public static void setUpClass() {
		re = new RelationExtractor();
	}

	public TestRelEx()
	{
		pass = 0;
		fail = 0;
		subpass = 0;
		subfail = 0;
	}

	public ArrayList<String> split(String a)
	{
		String[] sa = a.split("\n");
		ArrayList<String> saa = new ArrayList<String>();
		for (String s : sa) {
			saa.add(s);
		}
		Collections.sort (saa);
		return saa;
	}

	/**
	 * First argument is the sentence.
	 * Second argument is a list of the relations that RelEx
	 * should be generating.
	 * Return true if RelEx generates the same dependencies
	 * as the second argument.
	 */
	public boolean test_sentence (String sent, String sf)
	{
		re.do_penn_tagging = false;
		re.setMaxParses(1);
		Sentence sntc = re.processSentence(sent);
		ParsedSentence parse = sntc.getParses().get(0);
		String rs = SimpleView.printBinaryRelations(parse);
		String urs = SimpleView.printUnaryRelations(parse);

		ArrayList<String> exp = split(sf);
		ArrayList<String> brgot = split(rs);
		ArrayList<String> urgot = split(urs);

		// add number of binary relations from parser-output,
		// to total number of relationships gotten
		int sizeOfGotRelations= brgot.size();
		// check expected binary and unary relations
		// the below for-loop checks whether all expected binary relations are
		// contained in the parser-binary-relation-output arrayList "brgot".
		// if any unary relations are expected in the output it checks the
		// parser-unary-relation-output arrayList "urgot" for unary relationships
		for (int i=0; i< exp.size(); i++)
		{
			if(!brgot.contains(exp.get(i)))
			{
				if(!urgot.contains(exp.get(i)))
				{
					System.err.println("Error: content miscompare:\n" +
						    "\tExpected = " + exp + "\n" +
						    "\tGot Binary Relations = " + brgot + "\n" +
						    "\tGot Unary Relations = " + urgot + "\n" +
						    "\tSentence = " + sent);
					subfail ++;
					fail ++;
					sentfail.add(sent);
					return false;
				}
				// add the unary relation, count to total number of
				// binary relations
				sizeOfGotRelations++;
			}

		}
		// The size checking of the expected relationships vs output
		// relationships is done here purposefully, to accommodate if
		// there is any unary relationships present in the expected
		// output(see above for-loop also).  However it only checks
		// whether parser-output resulted more relationships(binary+unary)
		// than expected relations.  If the parser-output resulted in
		// fewer relationships(binary+unary) than expected it would
		// catch that in the above for-loop.
		if (exp.size() < sizeOfGotRelations)
		{
			System.err.println("Error: size miscompare:\n" +
					    "\tExpected = " + exp + "\n" +
					    "\tGot Binary Relations = " + brgot + "\n" +
					    "\tGot Unary Relations = " + urgot + "\n" +
					    "\tSentence = " + sent);
			subfail ++;
			fail ++;
			sentfail.add(sent);
			return false;
		}

		subpass ++;
		pass ++;
		return true;
	}

	public void report(boolean rc, String subsys)
	{
		if (rc) {
			System.err.println(subsys + ": Tested " + pass +
			                   " sentences, test passed OK");
		} else {
			System.err.println(subsys + ": Test failed\n\t" +
			                   fail + " sentences failed\n\t" +
			                   pass + " sentences passed");
		}
		subpass = 0;
		subfail = 0;
	}

	public boolean test_determiners()
	{
		boolean rc = true;
		rc &= test_sentence ("Ben ate my cookie.",
		                     "_subj(eat, Ben)\n" +
		                     "_obj(eat, cookie)\n" +
		                     "_poss(cookie, me)\n");
		rc &= test_sentence ("Ben ate that cookie.",
		                     "_subj(eat, Ben)\n" +
		                     "_obj(eat, cookie)\n" +
		                     "_det(cookie, that)\n");
		rc &= test_sentence ("All my writings are bad.",
		                     "_predet(writings, all)\n" +
		                     "_poss(writings, me)\n" +
		                     "_predadj(writings, bad)\n");
		rc &= test_sentence ("All his designs are bad.",
		                     "_predet(design, all)\n" +
		                     "_poss(design, him)\n" +
		                     "_predadj(design, bad)\n");
		rc &= test_sentence ("All the boys knew it.",
		                     "_subj(know, boy)\n" +
		                     "_obj(know, it)\n" +
		                     "_predet(boy, all)\n");

		rc &= test_sentence ("Joan thanked Susan for all the help she had given.",
		                     "for(thank, help)\n" +
		                     "_subj(thank, Joan)\n" +
		                     "_obj(thank, Susan)\n" +
		                     "_predet(help, all)\n" +
		                     "_subj(give, she)\n" +
		                     "_obj(give, help)\n");

		report(rc, "Determiners");
		return rc;
	}

	public boolean test_time()
	{
		boolean rc = true;
		rc &= test_sentence("I had breakfast at 8 am.",
				    "_obj(have, breakfast)\n"+
				    "at(have, am)\n" +
				    "_subj(have, I)\n" +
				    "_time(am, 8)\n");
		rc &= test_sentence("I had supper before 6 pm.",
				    "_obj(have, supper)\n" +
				    "before(have, pm)\n" +
				    "_subj(have, I)\n" +
				    "_time(pm, 6)\n");

		report(rc, "Time");
		return rc;
	}

	public boolean test_comparatives()
	{
		boolean rc = true;
		rc &= test_sentence ("Some people like pigs less than dogs.",
		                     "_advmod(like, less)\n" +
		                     "_obj(like, pig)\n" +
		                     "_quantity(people, some)\n" +
		                     "_subj(like, people)\n" +
		                     "than(pig, dog)\n");

		rc &= test_sentence ("Some people like pigs more than dogs.",
		                     "_advmod(like, more)\n" +
		                     "_obj(like, pig)\n" +
		                     "_quantity(people, some)\n" +
		                     "_subj(like, people)\n" +
		                     "than(pig, dog)\n");
		//Non-equal Gradable : Two entities one feature "more/less"

		rc &= test_sentence ("He is more intelligent than John.",
				    "than(he, John)\n" +
				    "_comparative(intelligent, he)\n" +
				    "degree(intelligent, comparative)\n"+
				    "_advmod(intelligent, more)\n"+
				    "_predadj(he, intelligent)\n");

		rc &= test_sentence ("He is less intelligent than John.",
				    "than(he, John)\n" +
				    "_comparative(intelligent, he)\n" +
				    "degree(intelligent, comparative)\n"+
				    "_advmod(intelligent, less)\n"+
				    "_predadj(he, intelligent)\n");

		rc &= test_sentence ("He runs more quickly than John.",
				    "_advmod(run, quickly)\n"+
				    "_advmod(quickly, more)\n"+
				    "_subj(run, he)\n" +
				    "than(he, John)\n" +
				    "_comparative(quickly, run)\n" +
				    "degree(quickly, comparative)\n");

		rc &= test_sentence ("He runs less quickly than John.",
				    "_advmod(run, quickly)\n" +
				    "_subj(run, he)\n" +
				    "_advmod(quickly, less)\n"+
				    "than(he, John)\n" +
				    "_comparative(quickly, run)\n" +
				    "degree(quickly, comparative)\n");

		rc &= test_sentence ("He runs more quickly than John does.",
				    "_advmod(run, quickly)\n" +
				    "_advmod(quickly, more)\n"+
				    "_subj(run, he)\n" +
				    "_subj(do, John)\n"+
				    "than(he, John)\n" +
				    "_comparative(quickly, run)\n" +
				    "degree(quickly, comparative)\n");

		// This sentence is ungrammatical but commonly used by
		// non-native English speakers
		rc &= test_sentence ("He runs less quickly than John does.",
				    "_advmod(run, quickly)\n" +
				    "_subj(run, he)\n" +
				    "_subj(do, John)\n"+
				    "_advmod(quickly, less)\n"+
				    "than(he, John)\n" +
				    "_comparative(quickly, run)\n" +
				    "degree(quickly, comparative)\n");

		rc &= test_sentence ("He runs slower than John does.",
				    "_advmod(run, slow)\n" +
				    "_subj(run, he)\n" +
				    "_subj(do, John)\n"+
				    "than(he, John)\n" +
				    "_comparative(slow, run)\n" +
				    "degree(slow, comparative)\n");

		rc &= test_sentence ("He runs more than John.",
				    "_obj(run, more)\n" +
				    "_subj(run, he)\n" +
				    "than(he, John)\n"+
				    "_comparative(more, run)\n"+
				    "degree(more, comparative)\n");

		rc &= test_sentence ("He runs less than John.",
				    "_obj(run, less)\n" +
				    "_subj(run, he)\n" +
				    "than(he, John)\n"+
				    "_comparative(less, run)\n"+
				    "degree(less, comparative)\n");

		rc &= test_sentence ("He runs faster than John.",
				    "than(he, John)\n" +
				    "_comparative(fast, run)\n" +
				    "_subj(run, he)\n"+
				    "_advmod(run, fast)\n"+
				    "degree(fast, comparative)\n");

		rc &= test_sentence ("He runs more slowly than John.",
				    "than(he, John)\n" +
				    "_subj(run, he)\n" +
				    "_advmod(slowly, more)\n"+
				    "_comparative(slowly, run)\n"+
				    "_advmod(run, slowly)\n"+
				    "degree(slowly, comparative)\n");

		rc &= test_sentence ("He runs less slowly than John.",
				    "than(he, John)\n" +
				    "_subj(run, he)\n" +
				    "_comparative(slowly, run)\n"+
				    "_advmod(run, slowly)\n"+
				    "_advmod(slowly, less)\n"+
				    "degree(slowly, comparative)\n");

		rc &= test_sentence ("He runs more miles than John does.",
				    "than(he, John)\n" +
				    "_subj(run, he)\n" +
				    "_subj(do, John)\n"+
				    "_obj(run, mile)\n"+
				    "_comparative(mile, run)\n"+
				    "_quantity(mile, more)\n"+
				    "degree(more, comparative)\n");

		rc &= test_sentence ("He runs fewer miles than John does.",
				    "than(he, John)\n" +
				    "_subj(run, he)\n" +
				    "_subj(do, John)\n"+
				    "_obj(run, mile)\n"+
				    "_comparative(mile, run)\n"+
				    "_quantity(mile, fewer)\n"+
				    "degree(fewer, comparative)\n");

		rc &= test_sentence ("He runs many more miles than John does.",
				    "than(he, John)\n" +
				    "_comparative(mile, run)\n"+
				    "_obj(run, mile)\n"+
				    "_subj(run, he)\n" +
				    "_subj(do, John)\n" +
				    "_quantity(mile, many)\n"+
				    "degree(more, comparative)\n");


		rc &= test_sentence ("He runs ten more miles than John.",
				    "_obj(run, mile)\n"+
				    "_subj(run, he)\n" +
				    "than(he, John)\n" +
				    "_comparative(mile, run)\n"+
				    "_quantity(mile, ten)\n" +
				    "numeric-FLAG(ten, T)\n" +
				    "degree(more, comparative)\n");

		rc &= test_sentence ("He runs almost ten more miles than John does.",
				    "_obj(run, mile)\n"+
				    "_subj(run, he)\n"+
				    "_comparative(mile, run)\n"+
				    "_subj(do, John)\n"+
				    "than(he, John)\n"+
				    "_quantity_mod(ten, almost)\n"+
				    "_quantity(mile, ten)\n"+
				    "numeric-FLAG(ten, T)\n" +
				    "degree(more, comparative)\n");

		rc &= test_sentence ("He runs more often than John.",
				    "_subj(run, he)\n"+
				    "_advmod(often, more)\n"+
				    "_advmod(run, often)\n"+
				    "_comparative(often, run)\n"+
				    "than(he, John)\n"+
				    "degree(often, comparative)\n");

		rc &= test_sentence ("He runs less often than John.",
				    "_subj(run, he)\n"+
				    "_advmod(often, less)\n"+
				    "_advmod(run, often)\n"+
				    "_comparative(often, run)\n"+
				    "than(he, John)\n"+
				    "degree(often, comparative)\n");

		rc &= test_sentence ("He runs here more often than John.",
				    "_advmod(run, here)\n"+
				    "_advmod(often, more)\n"+
				    "_advmod(run, often)\n"+
				    "_subj(run, he)\n"+
				    "_comparative(often, run)\n"+
				    "than(he, John)\n"+
				    "degree(often, comparative)\n");

		rc &= test_sentence ("He runs here less often than John.",
				    "_advmod(run, here)\n"+
				    "_advmod(often, less)\n"+
				    "_advmod(run, often)\n"+
				    "_subj(run, he)\n"+
				    "_comparative(often, run)\n"+
				    "than(he, John)\n"+
				    "degree(often, comparative)\n");

		rc &= test_sentence ("He is faster than John.",
				    "than(he, John)\n"+
				    "_predadj(he, fast)\n"+
				    "_comparative(fast, he)\n"+
				    "degree(fast, comparative)\n");

		rc &= test_sentence ("He is faster than John is.",
				    "than(he, John)\n"+
				    "_predadj(he, fast)\n"+
				    "_subj(be, John)\n"+
				    "_comparative(fast, he)\n"+
				    "degree(fast, comparative)\n");

		rc &= test_sentence ("His speed is faster than John's.",
				    "than(speed, be)\n"+
				    "_predadj(speed, fast)\n"+
				    "_poss(speed, him)\n"+
				    "_comparative(fast, speed)\n"+
				    "degree(fast, comparative)\n");

		rc &= test_sentence ("I run more than Ben.",
				    "_subj(run, I)\n"+
				    "_obj(run, more)\n"+
				    "_comparative(more, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(more, comparative)\n");

		rc &= test_sentence ("I run less than Ben.",
				    "_subj(run, I)\n"+
				    "_obj(run, less)\n"+
				    "_comparative(less, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(less, comparative)\n");

		rc &= test_sentence ("I run more miles than Ben.",
				    "_subj(run, I)\n"+
				    "_obj(run, mile)\n"+
				    "_quantity(mile, more)\n"+
				    "_comparative(mile, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(more, comparative)\n");

		rc &= test_sentence ("I run fewer miles than Ben.",
				    "_subj(run, I)\n"+
				    "_obj(run, mile)\n"+
				    "_quantity(mile, fewer)\n"+
				    "_comparative(mile, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(fewer, comparative)\n");

		rc &= test_sentence ("I run 10 more miles than Ben.",
				    "_subj(run, I)\n"+
				    "_obj(run, mile)\n"+
				    "_quantity(mile, 10)\n"+
				    "_comparative(mile, run)\n"+
				    "than(I, Ben)\n"+
				    "numeric-FLAG(10, T)\n" +
				    "degree(more, comparative)\n");

		rc &= test_sentence ("I run 10 fewer miles than Ben.",
				    "_subj(run, I)\n"+
				    "_obj(run, mile)\n"+
				    "_quantity(mile, 10)\n"+
				    "_comparative(mile, run)\n"+
				    "than(I, Ben)\n"+
				    "numeric-FLAG(10, T)\n" +
				    "degree(fewer, comparative)\n");

		rc &= test_sentence ("I run more often than Ben.",
				    "_subj(run, I)\n"+
				    "_advmod(run, often)\n"+
				    "_comparative(often, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(often, comparative)\n"+
				    "_advmod(often, more)\n");

		rc &= test_sentence ("I run less often than Ben.",
				    "_subj(run, I)\n"+
				    "_advmod(run, often)\n"+
				    "_comparative(often, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(often, comparative)\n"+
				    "_advmod(often, less)\n");

		rc &= test_sentence ("I run more often than Ben does.",
				    "_subj(run, I)\n"+
				    "_subj(do, Ben)\n"+
				    "_advmod(run, often)\n"+
				    "_comparative(often, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(often, comparative)\n"+
				    "_advmod(often, more)\n");

		rc &= test_sentence ("I run less often than Ben does.",
				    "_subj(run, I)\n"+
				    "_subj(do, Ben)\n"+
				    "_advmod(run, often)\n"+
				    "_comparative(often, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(often, comparative)\n"+
				    "_advmod(often, less)\n");

		rc &= test_sentence ("I run more often than Ben climbs.",
				    "_subj(run, I)\n"+
				    "_subj(climb, Ben)\n"+
				    "_comparative(often, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(often, comparative)\n"+
				    "_advmod(run, often)\n"+
				    "_advmod(often, more)\n");

		rc &= test_sentence ("I run less often than Ben climbs.",
				    "_subj(run, I)\n"+
				    "_subj(climb, Ben)\n"+
				    "_comparative(often, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(often, comparative)\n"+
				    "_advmod(run, often)\n"+
				    "_advmod(often, less)\n");

		rc &= test_sentence ("I run more races than Ben wins contests.",
				    "_subj(run, I)\n"+
				    "_obj(run, race)\n"+
				    "_subj(win, Ben)\n"+
				    "_obj(win, contest)\n"+
				    "_quantity(race, more)\n"+
				    "_comparative(race, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(more, comparative)\n");

		rc &= test_sentence ("I run fewer races than Ben wins contests.",
				    "_subj(run, I)\n"+
				    "_obj(run, race)\n"+
				    "_subj(win, Ben)\n"+
				    "_obj(win, contest)\n"+
				    "_quantity(race, fewer)\n"+
				    "_comparative(race, run)\n"+
				    "than(I, Ben)\n"+
				    "degree(fewer, comparative)\n");

		rc &= test_sentence ("I have more chairs than Ben.",
				    "_obj(have, chair)\n"+
				    "_subj(have, I)\n"+
				    "than(I, Ben)\n"+
				    "_comparative(chair, have)\n"+
				    "_quantity(chair, more)\n"+
				    "degree(more, comparative)\n");

		rc &= test_sentence ("I have fewer chairs than Ben.",
				    "_obj(have, chair)\n"+
				    "_subj(have, I)\n"+
				    "than(I, Ben)\n"+
				    "_comparative(chair, have)\n"+
				    "_quantity(chair, fewer)\n"+
				    "degree(fewer, comparative)\n");

		rc &= test_sentence ("He earns much more money than I do.",
				    "_obj(earn, money)\n"+
				    "_subj(do, I)\n"+
				    "_subj(earn, he)\n"+
				    "than(he, I)\n"+
				    "_comparative(money, earn)\n"+
				    "_quantity(money, more)\n"+
				    "_advmod(more, much)\n"+
				    "degree(more, comparative)\n");

		rc &= test_sentence ("He earns much less money than I do.",
				    "_obj(earn, money)\n"+
				    "_subj(do, I)\n"+
				    "_subj(earn, he)\n"+
				    "than(he, I)\n"+
				    "_comparative(money, earn)\n"+
				    "_quantity(money, less)\n"+
				    "_advmod(less, much)\n"+
				    "degree(less, comparative)\n");

		rc &= test_sentence ("She comes here more often than her husband.",
				    "_advmod(come, here)\n"+
				    "_advmod(often, more)\n"+
				    "_advmod(come, often)\n"+
				    "_subj(come, she)\n"+
				    "_poss(husband, her)\n"+
				    "_comparative(often, come)\n"+
				    "than(she, husband)\n"+
				    "degree(often, comparative)\n");

		rc &= test_sentence ("She comes here less often than her husband.",
				    "_advmod(come, here)\n"+
				    "_advmod(often, less)\n"+
				    "_advmod(come, often)\n"+
				    "_subj(come, she)\n"+
				    "_poss(husband, her)\n"+
				    "_comparative(often, come)\n"+
				    "than(she, husband)\n"+
				    "degree(often, comparative)\n");

		rc &= test_sentence ("Russian grammar is more difficult than English grammar.",
				    "_comparative(difficult, grammar)\n"+
				    "than(grammar, grammar)\n"+
				    "_amod(grammar, Russian)\n"+ //When link-grammar uses A, relex should use _amod it will use A instead of AN; will be  updated in next linkgrammer version
				    "_predadj(grammar, difficult)\n"+
				    "_amod(grammar, English)\n"+
				    "_advmod(difficult, more)\n"+
				    "degree(difficult, comparative)\n");

		rc &= test_sentence ("Russian grammar is less difficult than English grammar.",
				    "_comparative(difficult, grammar)\n"+
				    "than(grammar, grammar)\n"+
				    "_amod(grammar, Russian)\n"+
				    "_predadj(grammar, difficult)\n"+
				    "_amod(grammar, English)\n"+
				    "_advmod(difficult, less)\n"+
				    "degree(difficult, comparative)\n");

		rc &= test_sentence ("My sister is much more intelligent than me.",
				    "_amod(much, intelligent)\n"+
				    "_predadj(sister, intelligent)\n"+
				    "_poss(sister, me)\n"+
				    "than(sister, me)\n"+
				    "_comparative(intelligent, sister)\n"+
				    "_advmod(intelligent, more)\n"+
				    "degree(intelligent, comparative)\n");

		rc &= test_sentence ("My sister is much less intelligent than me.",
				    "_amod(much, intelligent)\n"+
				    "_predadj(sister, intelligent)\n"+
				    "_poss(sister, me)\n"+
				    "than(sister, me)\n"+
				    "_comparative(intelligent, sister)\n"+
				    "_advmod(intelligent, less)\n"+
				    "degree(intelligent, comparative)\n");

		rc &= test_sentence ("I find maths lessons more enjoyable than science lessons.",
				    "_iobj(find, maths)\n"+
				    "_obj(find, lesson)\n"+
				    "_subj(find, I)\n"+
				    "_amod(lesson, enjoyable)\n"+
				    "_nn(lesson, science)\n"+
				    "than(maths, science)\n"+
				    "_comparative(enjoyable, maths)\n"+
				    "_advmod(enjoyable, more)\n"+
				    "degree(enjoyable, comparative)\n");

		rc &= test_sentence ("I find maths lessons less enjoyable than science lessons.",
				    "_iobj(find, maths)\n"+
				    "_obj(find, lesson)\n"+
				    "_subj(find, I)\n"+
				    "_amod(lesson, enjoyable)\n"+
				    "_nn(lesson, science)\n"+
				    "than(maths, science)\n"+
				    "_comparative(enjoyable, maths)\n"+
				    "_advmod(enjoyable, less)\n"+
				    "degree(enjoyable, comparative)\n");

		// Comparatives Without More/less terms
		rc &= test_sentence ("Her great-grandson is nicer than her great-granddaughter.",
				    "than(great-grandson, great-granddaughter)\n"+
				    "_predadj(great-grandson, nice)\n"+
				    "_poss(great-grandson, her)\n"+
				    "_poss(great-granddaughter, her)\n"+
				    "_comparative(nice, great-grandson)\n"+
				    "degree(nice, comparative)\n");

		rc &= test_sentence ("George is cleverer than Norman.",
				    "than(George, Norman)\n"+
				    "_predadj(George, clever)\n"+
				    "_comparative(clever, George)\n"+
				    "degree(clever, comparative)\n");

		rc &= test_sentence ("Kim is taller than Linda.",
				    "than(Kim, Linda)\n"+
				    "_predadj(Kim, tall)\n"+
				    "_comparative(tall, Kim)\n"+
				    "degree(tall, comparative)\n");

		rc &= test_sentence ("Venus is brighter than Mars.",
		            "than(Venus, Mars)\n"+
				    "_predadj(Venus, bright)\n"+
				    "_comparative(bright, Venus)\n"+
				    "degree(bright, comparative)\n");

		rc &= test_sentence ("Mary is shorter than Jane.",
				    "than(Mary, Jane)\n"+
				    "_predadj(Mary, short)\n"+
				    "_comparative(short, Mary)\n"+
				    "degree(short, comparative)\n");

		rc &= test_sentence ("I am happier than you.",
				    "than(I, you)\n"+
				    "_predadj(I, happy)\n"+
				    "_comparative(happy, I)\n"+
				    "degree(happy, comparative)");

		rc &= test_sentence ("His house is bigger than hers.",
				    "than(house, hers)\n"+
				    "_predadj(house, big)\n"+
				    "_poss(house, him)\n"+
				    "_comparative(big ,house)\n"+
				    "degree(big, comparative)");

		rc &= test_sentence ("She is two years older than me.",
				    "_obj(is, year)\n"+
				    "_amod(years, old)\n"+
				    "_quantity(year, two)\n"+
				    "numeric-FLAG(two, T)\n" +
				    "than(she, me)\n"+
				    "_comparative(old, she)\n"+
				    "degree(old, comparative)");

		rc &= test_sentence ("New York is much bigger than Boston.",
				    "_subj(is, New_York)\n"+
				    "_amod(much, big)\n"+
				    "than(New_York, Boston)\n"+
				    "_comparative(big, New_York)\n"+
				    "degree(big, comparative)");

		rc &= test_sentence ("He is a better player than Ronaldo.",
				    "_obj(be, player)\n"+
				    "_subj(be, he)\n"+
				    "_amod(player, good)\n"+
				    "than(he, Ronaldo)\n"+
				    "_comparative(good, he)\n"+
				    "degree(good, comparative)");

		rc &= test_sentence ("France is a bigger country than Britain.",
				    "_obj(is, country)\n"+
				    "_subj(is, France)\n"+
				    "_amod(country, big)\n"+
				    "than(France, Britain)\n"+
				    "_comparative(big, France)\n"+
				    "degree(big, comparative)\n");

		rc &= test_sentence ("That joke was funnier than his joke.",
				    "_predadj(joke, funny)\n"+
				    "than(joke, joke)\n"+
				    "_det(joke, that)\n"+
				    "_poss(joke, him)\n"+
				    "_comparative(funny, joke)\n"+
				    "degree(funny, comparative)");

		rc &= test_sentence ("Our car is bigger than your car.",
				    "than(car, car)\n"+
				    "_predadj(car, big)\n"+
				    "_poss(car, us)\n"+
				    "_det(car, you)\n"+
				    "_poss(car, you)\n"+
				    "_comparative(big, car)\n"+
				    "degree(big, comparative)");
		// Sentences need to check
		rc &= test_sentence ("This computer is better than that one.",
				    "than(computer, one)\n"+
				    "_det(computer, this)\n"+
				    "_predadj(computer, good)\n"+
				    "_det(one, that)\n"+
				    "degree(good, comparative)\n"+
				    "_comparative(good, computer)\n");

		rc &= test_sentence ("He's simpler than I thought.",
				    "than(he, I)\n"+
				    "_subj(think, I)\n"+
				    "_comparative(simple, he)\n"+
				    "_predadj(he, simple)\n"+
				    "degree(simple, comparative)\n");

		rc &= test_sentence ("She's stronger at chess than I am.",
				    "at(strong, chess)\n"+
				    "than(she, I)\n"+
				    "_predadj(she, strong)\n"+
				    "degree(strong, comparative)\n"+
				    "_comparative(strong, she)\n");

		rc &= test_sentence ("She's prettier than her mother.",
				    "_predadj(she, pretty)\n"+
				    "than(she, mother)\n"+
				    "_poss(mother, her)\n"+
				    "_comparative(pretty, she)\n"+
				    "degree(pretty, comparative)\n");

		rc &= test_sentence ("This exam was more difficult than the other.",
				    "than(exam, other)\n"+
				    "_det(exam, this)\n"+
				    "_predadj(exam, difficult)\n"+
				    "_advmod(difficult, more)\n"+
				    "_comparative(difficult, exam)\n"+
				    "degree(difficult, comparative)\n");

		rc &= test_sentence ("It's much colder today than it was yesterday.",
				    "_subj(be, it)\n"+
				    "than(today, yesterday)\n"+
				    "_advmod(cold, today)\n"+
				    "_advmod(cold, yesterday)\n"+
				    "_predadj(it, cold)\n"+
				    "_comparative(cold, it)\n"+
				    "degree(cold, comparative)\n");

		rc &= test_sentence ("This grammar topic is easier than most others.",
				    "than(topic, others)\n"+
				    "_det(topic, this)\n"+
				    "_nn(topic, grammar)\n"+
				    "_predadj(topic, easy)\n"+
				    "_quantity(others, most)\n"+
				    "_comparative(easy, topic)\n"+
				    "degree(easy, comparative)\n");

		rc &= test_sentence ("I find science more difficult than mathematics.",
				    "_obj(find, science)\n"+
				    "_subj(find, I)\n"+
				    "_advmod(difficult, more)\n"+
				    "than(science, mathematics)\n"+
				    "_comparative(difficult, science)\n"+
				    "degree(difficult, comparative)\n");

		//one entity two or more features
		rc &= test_sentence ("He is more intelligent than attractive.",
				    "than(intelligent, attractive)\n"+
				    "_predadj(he, intelligent)\n"+
				    "_advmod(intelligent, more)\n"+
				    "_comparative(intelligent, he)\n"+
				    "degree(intelligent, comparative)\n");

		rc &= test_sentence ("He is less intelligent than attractive.",
				    "than(intelligent, attractive)\n"+
				    "_predadj(he, intelligent)\n"+
				    "_advmod(intelligent, less)\n"+
				    "_comparative(intelligent, he)\n"+
				    "degree(intelligent, comparative)\n");

		rc &= test_sentence ("The dog was more hungry than angry.",
				    "_predadj(dog, hungry)\n"+
				    "than(hungry, angry)\n"+
				    "_advmod(hungry, more)\n"+
				    "_comparative(hungry, dog)\n"+
				    "degree(hungry, comparative)\n");

		rc &= test_sentence ("The dog was less hungry than angry.",
				    "_predadj(dog, hungry)\n"+
				    "than(hungry, angry)\n"+
				    "_advmod(hungry, less)\n"+
				    "_comparative(hungry, dog)\n"+
				    "degree(hungry, comparative)\n");

		rc &= test_sentence ("He did it more quickly than carefully.",
				    "_obj(do, it)\n"+
				    "_subj(do, he)\n"+
				    "than(quickly, carefully)\n"+
				    "_advmod(do, quickly)\n"+
				    "_advmod(quickly, more)\n"+
				    "_comparative(quickly, do)\n"+
				    "degree(quickly, comparative)\n");

		rc &= test_sentence ("He did it less quickly than carefully.",
				    "_obj(do, it)\n"+
				    "_subj(do, he)\n"+
				    "than(quickly, carefully)\n"+
				    "_advmod(do, quickly)\n"+
				    "_advmod(quickly, less)\n"+
				    "_comparative(quickly, do)\n"+
				    "degree(quickly, comparative)\n");

		rc &= test_sentence ("He has more money than time.",
				    "_obj(have, money)\n"+
				    "_subj(have, he)\n"+
				    "than(money, time)\n"+
				    "_quantity(money, more)\n"+
				    "_comparative(money, have)\n"+
				    "degree(more, comparative)\n");

		rc &= test_sentence ("He has less money than time.",
				    "_obj(have, money)\n"+
				    "_subj(have, he)\n"+
				    "than(money, time)\n"+
				    "_quantity(money, less)\n"+
				    "_comparative(money, have)\n"+
				    "degree(less, comparative)\n");

		rc &= test_sentence ("He plays more for money than for pleasure.",
				    "_subj(play, he)\n"+
				    "_obj(play, more)\n"+
				    "for(play, money)\n"+
				    "for(than, pleasure)\n"+
				    "than(money, pleasure)\n"+
				    "_comparative(more, play)\n"+
				    "degree(more, comparative)\n");

		rc &= test_sentence ("He plays less for money than for pleasure.",
				    "_subj(play, he)\n"+
				    "_obj(play, less)\n"+
				    "for(play, money)\n"+
				    "for(than, pleasure)\n"+
				    "than(money, pleasure)\n"+
				    "_comparative(less, play)\n"+
				    "degree(less, comparative)\n");
		
		//two entities two features
		rc &= test_sentence ("Jack is more ingenious than Ben is crazy.", 
				    "_predadj(Jack, ingenious)\n"+
				    "_predadj(Ben, crazy)\n"+
				    "_advmod(ingenious, more)\n"+
				    "_comparative(ingenious, Jack)\n"+
				    "than(Jack, Ben)\n"+
				    "than1(ingenious, crazy)\n"+
				    "degree(ingenious, comparative)\n");
		
		rc &= test_sentence ("Jack is less ingenious than Ben is crazy.", 
				    "_predadj(Jack, ingenious)\n"+
				    "_predadj(Ben, crazy)\n"+
				    "_advmod(ingenious, less)\n"+
				    "_comparative(ingenious, Jack)\n"+
				    "than(Jack, Ben)\n"+
				    "than1(ingenious, crazy)\n"+
				    "degree(ingenious, comparative)\n");

		//two entities two features Without More/less
		rc &= test_sentence ("I slept longer than he worked",
				    "_subj(sleep, I)\n"+
				    "_subj(work, he)\n"+
				    "_advmod(sleep, long)\n"+
				    "than(I, he)\n"+
				    "than1(sleep, work)\n"+
				    "_comparative(long, sleep)\n"+
				    "degree(long, comparative)\n");

		report(rc, "Comparatives");
		return rc;
	}
	
		public boolean test_equatives()
	{
		boolean rc = true;
		//Equative:two entities one feature
		rc &= test_sentence ("Amen's hair is as long as Ben's.",
			"_poss(hair, Amen)\n"+
			"_predadj(hair, long)\n"+
			"as(long, Ben)\n"+
			"than(Amen, Ben)\n");

		rc &= test_sentence ("Amen’s hair is same as Ben’s.",
			"_poss(hair, Amen)\n"+
			"_predadj(hair, same)\n"+
			"as(same, Ben)\n"+
			"than(Amen, Ben)\n");

		rc &= test_sentence ("Jack’s hair color is similar to that of Ben’s.",
			"_poss(color, Jack)\n"+
			"_nn(color, hair)\n"+
			"_predadj(color, similar)\n"+
			"of(that, Ben)\n"+
			"to(similar, that)\n"+
			"than(Jack, Ben)\n");

		rc &= test_sentence ("Jack’s hair color is similar to Ben's",
			"_poss(color, Jack)\n"+
			"_nn(color, hair)\n"+
			"_predadj(color, similar)\n"+
			"to(similar, Ben)\n"+
			"than(Jack, Ben)\n");
				
		rc &= test_sentence ("Jack is as intelligent as Ben.",
			"_predadj(Jack, intelligent)\n"+
			"as(intelligent, Ben)\n"+
			"than(Jack, Ben)\n");

		rc &= test_sentence ("The book’s color is same as that of the pen’s.",
			"_poss(color, book)\n"+
			"_predadj(color, same)\n"+
			"of(that, pen)\n"+
			"as(same, that)\n"+
			"than(book, pen)\n");

		rc &= test_sentence ("The snail is running  exactly as fast as the cheetah.",
			"_predadj(snail, run)\n"+
			"as(run, cheetah)\n"+
			"_advmod(fast, exactly)\n"+
			"than(snail, cheetah)\n");
		
		//one entity one feature, through time
		rc &= test_sentence ("The coffee tastes the same as it did last year.",
			"_subj(taste, coffee)\n"+
			"_obj(taste, same)\n"+
			"_obj(do, year)\n"+
			"_subj(do, it)\n"+
			"as(taste, do)\n"+
			"_amod(year, last)\n");

		rc &= test_sentence ("The coffee tastes as it did last year.",
			"_subj(taste, coffee)\n"+
			"_obj(do, year)\n"+
			"_subj(do, it)\n"+
			"as(taste, do)\n"+
			"_amod(year, last)\n");

		rc &= test_sentence ("Mike runs as fast as he did last year.",
			"_subj(do, he)\n"+
			"_subj(run, Mike)\n"+
			"as(fast, he)\n"+
			"_advmod(run, fast)\n"+
			"_advmod(do, year)\n"+
			"_amod(year, last)\n"+
			"than(Mike, he)\n");

		rc &= test_sentence ("The kick was as soft as the first.",
			"_predadj(kick, soft)\n"+
			"as(kick, first)\n");

		rc &= test_sentence ("He is as smart as I ever expected him to be.",
			"_predadj(he, smart)\n"+
			"_subj(expect, I)\n"+
			"_obj(expect, him)\n"+
			"as(smart, expect)\n"+
			"_advmod(expect, ever)\n"+
			"_to-do(smart, be)\n");
			
		report(rc, "Equatives");
		return rc;
	}

	public boolean test_conjunctions()
	{
		boolean rc = true;
		// conjoined verbs
		rc &= test_sentence ("Scientists make observations and ask questions.",
		                     "_obj(make, observation)\n" +
		                     "_obj(ask, question)\n" +
		                     "_subj(make, scientist)\n" +
		                     "_subj(ask, scientist)\n" +
		                     "conj_and(make, ask)\n");
		// conjoined nouns
		rc &= test_sentence ("She is a student and an employee.",
		                     "_obj(be, student)\n" +
		                     "_obj(be, employee)\n" +
		                     "_subj(be, she)\n" +
		                     "conj_and(student, employee)\n");
		// conjoined adjectives
		rc &= test_sentence ("I hailed a black and white taxi.",
		                     "_obj(hail, taxi)\n" +
		                     "_subj(hail, I)\n" +
		                     "_amod(taxi, black)\n" +
		                     "_amod(taxi, white)\n" +
		                     "conj_and(black, white)\n");
		// conjoined adverbs
		rc &= test_sentence ("She ran quickly and quietly.",
		                     "_advmod(run, quickly)\n" +
		                     "_advmod(run, quietly)\n" +
		                     "_subj(run, she)\n" +
		                     "conj_and(quickly, quietly)\n");
		// adjectival modifiers on conjoined subject
		rc &= test_sentence ("The big truck and the little car collided.",
		                     "_amod(car, little)\n" +
		                     "_amod(truck, big)\n" +
		                     "_subj(collide, truck)\n" +
		                     "_subj(collide, car)\n" +
		                     "conj_and(truck, car)\n");
		// verbs with modifiers
		rc &= test_sentence ("We ate dinner at home and went to the movies.",
		                     "_obj(eat, dinner)\n" +
		                     "conj_and(eat, go)\n" +
		                     "at(eat, home)\n" +
		                     "_subj(eat, we)\n" +
		                     "to(go, movie)\n" +
		                     "_subj(go, we)\n");
		// verb with more modifiers
		rc &= test_sentence ("We ate a late dinner at home and went out to the movies afterwards.",
		                     "_obj(eat, dinner)\n" +
		                     "conj_and(eat, go_out)\n" +
		                     "at(eat, home)\n" +
		                     "_subj(eat, we)\n" +
		                     "to(go_out, movie)\n" +
		                     "_advmod(go_out, afterwards)\n" +
		                     "_subj(go_out, we)\n" +
		                     "_amod(dinner, late)\n");

		// conjoined ditransitive verbs
		rc &= test_sentence ("She baked him a cake and sang him a song.",
		                     "_iobj(sing, him)\n" +
		                     "_obj(sing, song)\n" +
		                     "_subj(sing, she)\n" +
		                     "_iobj(bake, him)\n" +
		                     "_obj(bake, cake)\n" +
		                     "conj_and(bake, sing)\n" +
		                     "_subj(bake, she)\n");
		// conjoined adverbs with modifiers
		rc &= test_sentence ("she ran very quickly and extremely quietly.",
		                     "_advmod(run, quickly)\n" +
		                     "_advmod(run, quietly)\n" +
		                     "_subj(run, she)\n" +
		                     "_advmod(quietly, extremely)\n" +
		                     "conj_and(quickly, quietly)\n" +
		                     "_advmod(quickly, very)\n");
		// conjoined adverbs with out modifiers
		rc &= test_sentence ("She handled it quickly and gracefully.",
		                     "_obj(handle, quickly)\n" +
		                     "_obj(handle, gracefully)\n" +
		                     "_advmod(handle, quickly)\n" +
		                     "_advmod(handle, gracefully)\n" +
		                     "_subj(handle, she)\n" +
		                     "conj_and(quickly, gracefully)\n");
		// modifiers on conjoined adjectives
		rc &= test_sentence ("He had very long and very white hair.",
		                     "_obj(have, hair)\n" +
		                     "_subj(have, he)\n" +
		                     "_amod(hair, long)\n" +
		                     "_amod(hair, white)\n" +
		                     "_advmod(white, very)\n" +
		                     "conj_and(long, white)\n" +
		                     "_advmod(long, very)\n");
		// adjectival modifiers on conjoined object
		rc &= test_sentence ("The collision was between the little car and the big truck.",
		                     "_pobj(between, car)\n" +
		                     "_pobj(between, truck)\n" +
		                     "_psubj(between, collision)\n" +
		                     "_amod(truck, big)\n" +
		                     "_amod(car, little)\n" +
		                     "conj_and(car, truck)\n");
		// Names Modifiers and conjunction
		rc &= test_sentence ("Big Tom and Angry Sue went to the movies.",
		                     "to(go, movie)\n" +
		                     "_subj(go, Big_Tom)\n" +
		                     "_subj(go, Angry_Sue)\n" +
		                     "conj_and(Big_Tom, Angry_Sue)\n");
                //Correlative conjunction
                rc &= test_sentence ("I could use neither the lorry nor the van.",
                            "_to-do(could, use)\n"+
                            "_quantity(lorry, neither)\n"+
                            "conj_neither_nor(lorry, van)\n"+
                            "_obj(use, lorry)\n"+
                            "_obj(use, van)\n"+
                            "_subj(use, I)\n");

		report(rc, "Conjunction");
		return rc;
	}

	public boolean test_extraposition()
	{
		boolean rc = true;
		rc &= test_sentence ("The woman who lives next door is a registered nurse.",
		                        "_obj(be, nurse)\n" +
		                        "_subj(be, woman)\n" +
		                        "_amod(nurse, registered)\n" +
		                        "_advmod(live, next_door)\n" +
		                        "_subj(live, woman)\n" +
		                        "who(woman, live)\n");

		rc &= test_sentence ("A player who is injured has to leave the field.",
		                        "_to-do(have, leave)\n" +
		                        "_subj(have, player)\n" +
		                        "_obj(leave, field)\n" +
		                        "_predadj(player, injured)\n" +
		                        "who(player, injured)\n" );

		rc &= test_sentence ("Pizza, which most people love, is not very healthy.",
		                        "_advmod(very, not)\n" +
		                        "_advmod(healthy, very)\n" +
		                        "_obj(love, Pizza)\n" +
		                        "_quantity(people, most)\n" +
		                        "which(Pizza, love)\n" +
		                        "_subj(love, people)\n" +
		                        "_predadj(Pizza, healthy)\n" );

		rc &= test_sentence ("The restaurant which belongs to my aunt is very famous.",
		                        "_advmod(famous, very)\n" +
		                        "to(belong, aunt)\n" +
		                        "_subj(belong, restaurant)\n" +
		                        "_poss(aunt, me)\n" +
		                        "which(restaurant, belong)\n" +
		                        "_predadj(restaurant, famous)\n");

		rc &= test_sentence ("The books which I read in the library were written by Charles Dickens.",
		                        "_obj(write, book)\n" +
		                        "by(write, Charles_Dickens)\n" +
		                        "_obj(read, book)\n" +
		                        "in(read, library)\n" +
		                        "_subj(read, I)\n" +
		                        "which(book, read)\n");

		rc &= test_sentence("This is the book whose author I met in a library.",
		                       "_obj(be, book)\n" +
		                       "_subj(be, this)\n" +
		                       "_obj(meet, author)\n" +
		                       "in(meet, library)\n" +
		                       "_subj(meet, I)\n" +
		                       "whose(book, author)\n");

		rc &= test_sentence("The book that Jack lent me is very boring.",
		                       "_advmod(boring, very)\n" +
		                       "_iobj(lend, book)\n" +
		                       "_obj(lend, me)\n" +
		                       "_subj(lend, Jack)\n" +
		                       "that(book, lend)\n" +
		                       "_predadj(book, boring)\n");

		rc &= test_sentence("They ate a special curry which was recommended by the restaurant’s owner.",
		                       "_obj(eat, curry)\n" +
		                       "_subj(eat, they)\n" +
		                       "_obj(recommend, curry)\n" +
		                       "by(recommend, owner)\n" +
		                       "_poss(owner, restaurant)\n" +
		                       "which(curry, recommend)\n" +
		                       "_amod(curry, special)\n");

		rc &= test_sentence("The dog who Jack said chased me was black.",
		                       "_obj(chase, me)\n" +
		                       "_subj(chase, dog)\n" +
		                       "_subj(say, Jack)\n" +
		                       "_predadj(dog, black)\n" +
		                       "who(dog, chase)\n");

		rc &= test_sentence("Jack, who hosted the party, is my cousin.",
		                       "_obj(be, cousin)\n" +
		                       "_subj(be, Jack)\n" +
		                       "_poss(cousin, me)\n" +
		                       "_obj(host, party)\n" +
		                       "_subj(host, Jack)\n" +
		                       "who(Jack, host)\n");

		rc &= test_sentence("Jack, whose name is in that book, is the student near the window.",
		                       "near(be, window)\n" +
		                       "_obj(be, student)\n" +
		                       "_subj(be, Jack)\n" +
		                       "_pobj(in, book)\n" +
		                       "_psubj(in, name)\n" +
		                       "_det(book, that)\n" +
		                       "whose(Jack, name)\n");

		rc &= test_sentence("Jack stopped the police car that was driving fast.",
		                       "_obj(stop, car)\n" +
		                       "_subj(stop, Jack)\n" +
		                       "_advmod(drive, fast)\n" +
		                       "_predadj(car, drive)\n" +
		                       "that(car, drive)\n" +
		                       "_nn(car, police)\n");

		rc &= test_sentence("Just before the crossroads, the car was stopped by a traffic sign that stood on the street.",
		                       "_obj(stop, car)\n" +
		                       "by(stop, sign)\n" +
		                       "_advmod(stop, just)\n" +
		                       "on(stand, street)\n" +
		                       "_subj(stand, sign)\n" +
		                       "that(sign, stand)\n" +
		                       "_nn(sign, traffic)\n" +
		                       "before(just, crossroads)\n");

		report(rc, "Extrapostion");
		return rc;
	}
	
	//Added by Matthew
	public boolean test_inquisitives()
	{
		boolean rc = true;
		rc &= test_sentence ("What is Socrates?",
							 "_obj(be, Socrates)\n" +
							 "_subj(be, _$qVar)\n");

		rc &= test_sentence ("Who is the teacher?",
							 "_obj(be, teacher)\n" +
		                     "_subj(be, _$qVar)\n");


		rc &= test_sentence ("Who is a man?",
		                     "_obj(be, man)\n" +
		                     "_subj(be, _$qVar)\n");

		rc &= test_sentence ("Who told you that bullshit?", 
		                     "_iobj(tell, you)\n" +
		                     "_obj(tell, bullshit)\n" +
		                     "_subj(tell, _$qVar)\n" +
		                     "_det(bullshit, that)\n");

		rc &= test_sentence ("Who told that story to the police?", 
		                     "to(tell, police)\n" +
		                     "_obj(tell, story)\n" +
		                     "_subj(tell, _$qVar)\n" +
		                     "_det(story, that)\n");

		rc &= test_sentence ("What gives you that idea?", 
		                     "_iobj(give, you)\n" +
		                     "_obj(give, idea)\n" +
		                     "_subj(give, _$qVar)\n" +
		                     "_det(idea, that)\n");

		rc &= test_sentence ("What gave that idea to the police?",
		                     "to(give, police)\n" +
		                     "_obj(give, idea)\n" +
		                     "_subj(give, _$qVar)\n" +
		                     "_det(idea, that)\n");

		rc &= test_sentence ("What did you tell the fuzz?", 
		                     "_iobj(tell, fuzz)\n" +
		                     "_obj(tell, _$qVar)\n" +
		                     "_subj(tell, you)\n");

		rc &= test_sentence ("What did you give to Mary?", 
		                     "to(give, Mary)\n" +
		                     "_obj(give, _$qVar)\n" +
		                     "_subj(give, you)\n");

		rc &= test_sentence ("Who did you give the slavers?", 
		                     "_subj(give, you)\n");

		rc &= test_sentence ("Who did you sell to the slavers?",
		                     "to(sell, to)\n" +
		                     "_subj(sell, you)\n");

		rc &= test_sentence ("To whom did you sell the children?", 
		                     "to(sell, _$qVar)\n" +
		                     "_obj(sell, child)\n" +
		                     "_subj(sell, you)\n");

		rc &= test_sentence ("To what do we owe the pleasure?", 
		                     "to(owe, _$qVar)\n" +
		                     "_obj(owe, pleasure)\n" +
							 "_subj(owe, we)\n");

		rc &= test_sentence ("Who did you sell the children to?",
		                     "to(sell, to)\n" +
		                     "_obj(sell, child)\n" +
		                     "_subj(sell, you)\n");

		rc &= test_sentence ("What bothers you?",
		                     "_obj(bother, you)\n" +
		                     "_subj(bother, _$qVar)\n"); 

		rc &= test_sentence ("Who programmed you?",
		                     "_obj(program, you)\n" +
		                     "_subj(program, _$qVar)\n");

		rc &= test_sentence ("What is on the table?",
		                     "_pobj(on, table)\n" +
		                     "_psubj(on, _$qVar)\n");

		rc &= test_sentence ("What did you say?", 
		                     "_obj(say, _$qVar)\n" +
		                     "_subj(say, you)\n");

		rc &= test_sentence ("Who do you love?",
		                     "_obj(love, _$qVar)\n" +
		                     "_subj(love, you)\n");

		rc &= test_sentence ("What is for dinner?",
		                     "_pobj(for, dinner)\n" +
		                     "_psubj(for, _$qVar)\n");

		rc &= test_sentence ("Who's on first?",
		                     "_pobj(on, first)\n" +
		                     "_psubj(on, _$qVar)\n");
		
		rc &= test_sentence ("Who farted?",
		                     "_subj(fart, _$qVar)\n");

		rc &= test_sentence ("What is happening?", 
		                     "_subj(happen, _$qVar)\n");

		rc &= test_sentence ("Who is correct?", 
		                     "_predadj(_$qVar, correct)\n");

		rc &= test_sentence ("What is right?",
		                     "_predadj(_$qVar, right)\n");
				
		rc &= test_sentence ("What are you doing?",
		                     "_subj(_$qVar, you)\n");

		rc &= test_sentence ("Are you the one?",
		                     "_obj(be, one)\n" +
		                     "_subj(be, you)\n");
		
		rc &= test_sentence ("Are you mad?", 
		                     "_predadj(you, mad)\n");
	
		rc &= test_sentence ("Is the book under the table?",
		                     "under(book, table)\n" +
							 "_subj(be, book)\n");
	
		rc &= test_sentence ("Does he seem mad?",
		                     "_to-be(seem, mad)\n" +
		                     "_subj(seem, he)\n");

		rc &= test_sentence ("Does she want to help us?",	
		                     "_obj(help, us)\n" +
		                     "_to-do(want, help)\n" +
		                     "_subj(want, she)\n");		

		rc &= test_sentence ("Does she want you to help us?",
		                     "_obj(help, us)\n" +
		                     "_subj(help, you)\n" +
		                     "_to-do(want, help)\n" +
		                     "_subj(want, she)\n");

		rc &= test_sentence ("Was she good enough to help?",
		                     "_predadj(she, good)\n" +
		                     "_to-do(good, help)\n");

		rc &= test_sentence ("Must she be able to sing?",
		                     "_to-do(able, sing)\n" +
		                     "_predadj(she, able)\n");

		rc &= test_sentence ("Does she want to sing?",
		                     "_to-do(want, sing)\n" +
		                     "_subj(want, she)\n");

		rc &= test_sentence ("Have you slept?",
		                     "_subj(sleep, you)\n");

		rc &= test_sentence ("Will you sleep?",
		                     "_subj(sleep, you)\n");
		
		rc &= test_sentence ("Did you sleep?",
		                     "_subj(sleep, you)\n");

		rc &= test_sentence ("Did you eat the leftover baba-ganoush?",
		                     "_obj(eat, leftover)\n" +
		                     "_to-be(eat, baba-ganoush)\n" +
		                     "_subj(eat, you)\n");

		rc &= test_sentence ("Did you give her the money?",
		                     "_iobj(give, her)\n" +
		                     "_obj(give, money)\n" +
		                     "_subj(give, you)\n");

		rc &= test_sentence ("Did you give the money to her?",
		                     "to(give, her)\n" +
		                     "_obj(give, money)\n" +
		                     "_subj(give, you)\n");

		rc &= test_sentence ("The book is under the table?",
		                     "_pobj(under, table)\n" +
		                     "_psubj(under, book)\n");

		rc &= test_sentence ("Maybe she eats lunch.",
		                     "_obj(eat, lunch)\n" +
		                     "_advmod(eat, maybe)\n" +
		                     "_subj(eat, she)\n");

		rc &= test_sentence ("Perhaps she is nice.",
		                     "_advmod(nice, perhaps)\n" +
		                     "_predadj(she, nice)\n");

		rc &= test_sentence ("She wants to help John.",
		                     "_to-do(want, help)\n" +
		                     "_subj(want, she)\n" +
		                     "_obj(help, John)\n");

		rc &= test_sentence ("She wants you to help us.",
		                     "_to-do(want, help)\n" +
		                     "_subj(want, she)\n" +
		                     "_obj(help, us)\n" +
		                     "_subj(help, you)\n");

		rc &= test_sentence ("She is nice to help with the project.", 
		                     "with(help, project)\n" +
		                     "_to-do(nice, help)\n" +
		                     "_predadj(she, nice)\n");

		rc &= test_sentence ("She must be able to sing.",
		                     "_to-do(able, sing)\n" +
		                     "_predadj(she, able)\n");

		rc &= test_sentence ("She must need to sing?", 
		                     "_to-do(need, sing)\n" +
		                     "_subj(need, she)\n");

		rc &= test_sentence ("She must want to sing?", 
		                     "_to-do(want, sing)\n" +
		                     "_subj(want, she)\n");

		rc &= test_sentence ("She wants to sing.",
		                     "_to-do(want, sing)\n" +
		                     "_subj(want, she)\n");

		rc &= test_sentence ("Where do you live?",
		                     "_%atLocation(live, _$qVar)\n" +
		                     "_subj(live, you)\n");

		rc &= test_sentence ("Where did you eat dinner?",
		                     "_%atLocation(eat, _$qVar)\n" +
		                     "_obj(eat, dinner)\n" +
		                     "_subj(eat, you)\n");

		rc &= test_sentence ("Where is the party?",
		                     "_%atLocation(_%copula, _$qVar)\n" +
		                     "_subj(_%copula, party)\n");

		rc &= test_sentence ("Where will she be happy?", 
		                     "_%atLocation(happy, _$qVar)\n" +
		                     "_predadj(she, happy)\n");

		rc &= test_sentence ("When did jazz die?",
		                     "_%atTime(die, _$qVar)\n" +
		                     "_subj(die, jazz)\n");

		rc &= test_sentence ("When did you bake the cake?",
		                     "_%atTime(bake, _$qVar)\n" +
		                     "_obj(bake, cake)\n" +
		                     "_subj(bake, you)\n");

		rc &= test_sentence ("When did you give him the money?", 
		                     "_iobj(give, him)\n" +
		                     "_%atTime(give, _$qVar)\n" +
		                     "_obj(give, money)\n" +
		                     "_subj(give, you)\n");

		rc &= test_sentence ("When is the party?",
		                     "_%atTime(_%copula, _$qVar)\n" +
		                     "_subj(_%copula, party)\n");

		rc &= test_sentence ("Why do you live?",
		                     "_subj(live, you)\n");

		rc &= test_sentence ("Why do you like terrible music?",
		                     "_obj(like, music)\n" +
		                     "_subj(like, you)\n" +
							 "_amod(music, terrible)\n");

		rc &= test_sentence ("Why are you such a fool?", 
		                     "_obj(be, fool)\n" +
		                     "_subj(be, you)\n");

		rc &= test_sentence ("How did you sleep?",
		                     "how(sleep, _$qVar)\n" +
		                     "_subj(sleep, you)\n");

		rc &= test_sentence ("How was the party?", 
		                     "how(_%copula, _$qVar)\n" +
							 "_subj(_%copula, party)\n");

		rc &= test_sentence ("How is your food?",
		                     "_poss(food, you)\n" +
		                     "how(_%copula, _$qVar)\n" +
		                     "_subj(_%copula, food)\n");

		rc &= test_sentence ("How much money does it cost?",
		                     "_obj(cost, money)\n" +
		                     "_subj(cost, it)\n" +
		                     "_quantity(money, _$qVar)\n");

		rc &= test_sentence ("How many books have you read?",
		                     "_obj(read, book)\n" +
		                     "_subj(read, you)\n" +
		                     "_quantity(book, _$qVar)\n");

		rc &= test_sentence ("How fast does it go?",
		                     "_advmod(fast, _$qVar)\n" +
		                     "_subj(go, it)\n");

		rc &= test_sentence ("Which girl do you like?",
		                     "_obj(like, girl)\n" +
		                     "_subj(like, you)\n" +
		                     "_quantity(girl, _$qVar)\n");

		rc &= test_sentence ("Which girl likes you?",
		                     "_obj(like, you)\n" +
		                     "_subj(like, girl)\n" +
		                     "_quantity(girl, _$qVar)\n");

		rc &= test_sentence ("Which girl is crazy?",
		                     "_quantity(girl, _$qVar)\n" +
		                     "_predadj(girl, crazy)\n");

		rc &= test_sentence ("The books were written by Charles Dickens.",
		                     "_obj(write, book)\n" +
		                     "by(write, Charles_Dickens)\n");

		rc &= test_sentence ("The books are published.",
		                     "_obj(publish, book)\n");

		rc &= test_sentence ("I did my homework, and I went to school.",
		                     "_obj(do, homework)\n" +
		                     "_subj(do, I)\n" +
		                     "to(go, school)\n" +
		                     "_subj(go, I)\n" +
		                     "_poss(homework, me)\n");

		rc &= test_sentence ("John and Madison eat the cake.",
		                     "_obj(eat, cake)\n" +
		                     "_subj(eat, John)\n" +
		                     "_subj(eat, Madison)\n" +
		                     "conj_and(John, Madison)\n");

		rc &= test_sentence ("Joan is poor  but  happy.", 
		                     "_predadj(Joan, poor)\n" +
		                     "_predadj(Joan, happy)\n");

		rc &= test_sentence ("I think that dogs can fly.",
		                     "that(think, fly)\n" +
		                     "_subj(think, I)\n" +
		                     "_subj(fly, dog)\n");

		rc &= test_sentence ("He is glad that she won.",
		                     "that(glad, win)\n" +
		                     "_subj(win, she)\n" +
		                     "_predadj(he, glad)\n");

		rc &= test_sentence ("He ran so quickly that he flew.",
		                     "_advmod(quickly, so)\n" +
		                     "_subj(fly, he)\n" +
		                     "that(run, fly)\n" +
		                     "_advmod(run, quickly)\n" +
		                     "_subj(run, he)\n");

		rc &= test_sentence ("I had dinner at 6 pm",
		                     "_obj(have, dinner)\n" +
		                     "at(have, pm)\n" +
		                     "_subj(have, I)\n" +
		                     "_time(pm, 6)\n");

		rc &= test_sentence ("I went to sleep at 1 am",
		                     "to(go, sleep)\n" +
		                     "_subj(go, I)\n" +
		                     "_time(am, 1)\n" +
		                     "at(sleep, am)\n");

		rc &= test_sentence ("Who farted?",
		                     "_subj(fart, _$qVar)\n");

		rc &= test_sentence ("What happened?",
		                     "_subj(happen, _$qVar)\n");

		rc &= test_sentence ("What killed him?",
		                     "_obj(kill, him)\n" +
		                     "_subj(kill, _$qVar)\n");

		rc &= test_sentence ("Who ate the pizza?",
		                     "_obj(eat, pizza)\n" +
		                     "_subj(eat, _$qVar)\n");

		rc &= test_sentence ("What gave you that idea?",
		                     "_iobj(give, you)\n" +
		                     "_obj(give, idea)\n" +
		                     "_subj(give, _$qVar)\n" +
		                     "_det(idea, that)\n");

		rc &= test_sentence ("Who told you that?",
		                     "_iobj(tell, you)\n" +
		                     "_obj(tell, that)\n" +
		                     "_subj(tell, _$qVar)\n");

		rc &= test_sentence ("What is for dinner?",
		                     "_pobj(for, dinner)\n" +
		                     "_psubj(for, _$qVar)\n");

		rc &= test_sentence ("Who's on first?",
		                     "_pobj(on, first)\n" +
		                     "_psubj(on, _$qVar)\n");

		rc &= test_sentence ("Who are you?",
		                     "_subj(_%copula, you)\n");

		rc &= test_sentence ("Who do you love?",
		                     "_obj(love, _$qVar)\n" +
		                     "_subj(love, you)\n");

		rc &= test_sentence ("What do you think?",
		                     "_obj(think, _$qVar)\n" +
		                     "_subj(think, you)\n");

		rc &= test_sentence ("To whom did you sell the children?",
		                     "to(sell, _$qVar)\n" +
		                     "_obj(sell, child)\n" +
		                     "_subj(sell, you)\n");

		rc &= test_sentence ("Why did you give him the money?",
		                     "_iobj(give, him)\n" +
							 "_obj(give, money)\n" +
							 "_subj(give, you)\n");

		rc &= test_sentence ("Why are you so stupid?",
		                     "_advmod(stupid, so)\n" +
		                     "_predadj(you, stupid)\n");

		rc &= test_sentence ("How did you like the movie?",
		                     "_obj(like, movie)\n" +
		                     "how(like, _$qVar)\n" +
		                     "_subj(like, you)\n");

		rc &= test_sentence ("How did you send him the message?",
		                     "_iobj(send, him)\n" +
		                     "_obj(send, message)\n" +
		                     "how(send, _$qVar)\n" +
		                     "_subj(send, you)\n");

		report(rc, "Inquisitives");
		return rc;
	}

	public static void main(String[] args)
	{
		setUpClass();
		TestRelEx ts = new TestRelEx();
		ts.runTests();
	}

	// @Test
	public void runTests() {
		TestRelEx ts = this;
		boolean rc = true;

		rc &= ts.test_determiners();
		rc &= ts.test_time();
		rc &= ts.test_comparatives();
		rc &= ts.test_equatives();
		rc &= ts.test_extraposition();
		rc &= ts.test_conjunctions();
		rc &= ts.test_inquisitives();

		if (rc) {
			System.err.println("Tested " + ts.pass + " sentences, test passed OK");
		} else {
			System.err.println("Test failed\n\t" +
			                   ts.fail + " sentences failed\n\t" +
			                   ts.pass + " sentences passed");
		}

		System.err.println("******************************");
		System.err.println("Failed test sentences on Relex");
		System.err.println("******************************");
		if (sentfail.isEmpty())
			System.err.println("All test sentences passed");
		for(String temp : sentfail){
			System.err.println(temp);
		}
		System.err.println("******************************\n");
	}
}
