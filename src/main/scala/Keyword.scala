import ruc.irm.xextractor.evaluation.EvalResult
import ruc.irm.xextractor.keyword.TextRankExtractor.GraphType._
import ruc.irm.xextractor.keyword.{TextRankExtractor, Word2VecKMeansExtractor}

/**
  * 利用Scala进行关键词抽取测试, 命令行下运行：sbt console
  * 然后执行：
  * ```Keyword test 1```, 表明是测试第一篇文档,
  *
  * 執行以下代碼，测试保留的关键词数量从１到１０时，准确率、召回率和Ｆ值的变化：
  * ```
  *   val results = Keyword.evaluateAll(1, 10)
  *   Keyword.printEvaluationResult(results, 1)
  * ```
  *
  *
  */
object Keyword {
  lazy val weightedExtractor: TextRankExtractor = new TextRankExtractor(PositionRank)
  lazy val ningExtractor: TextRankExtractor = new TextRankExtractor(NingJianfei)
  lazy val clusteringExtractor: TextRankExtractor = new TextRankExtractor(ClusterRank)
  lazy val kmeansExtractor = new Word2VecKMeansExtractor()

  /**
    * 对比测试第id篇文档在不同抽取方法下的抽取结果
    *
    */
  val compare = (id: Int) => {
    val topN = 5
    val article = Articles.getArticle(id)
    println("原始关键词：" + article("tags"))
    println("-------------------")
    println("词语位置加权：\t" + weightedExtractor.extractAsList(article("title"), article("content"), topN))
    println("宁建飞方法：\t" + ningExtractor.extractAsList(article("title"), article("content"), topN))
    println("夏天新方法：\t" + clusteringExtractor.extractAsList(article("title"), article("content"), topN))
    println("Word2Vec聚类：\t" + kmeansExtractor.extractAsList(article("title"), article("content"), topN))
    println()
  }

  val evaluate = (topN: Int) => ruc.irm.xextractor.evaluation.ExtractKeywordEvaluation.evaluate(topN)

  /**
    * 选择不同数量的topN关键词，评估抽取结果， Array里面保存的是不同方法的抽取结果
    */
  def evaluateAll(fromTopN: Int = 3, toTopN: Int = 10): IndexedSeq[Array[EvalResult]] = (fromTopN to toTopN) map evaluate

  def printEvaluationResult(results: IndexedSeq[Array[EvalResult]], fromTopN: Int = 3) = {
    results.zipWithIndex.foreach {
      case (evalResultArray, idx) => {
        println(s"Keywords count:${idx + fromTopN}")
        println("-------------------")
        evalResultArray foreach println
        println()
      }
    }
    println("-------END---------\n")
    results
  }

  /**
    * 输出LaTex格式的表格，方便论文写作，格式为：
    *     M1 & 0.304 & 0.259 & 0.277   & 0.000 & 0.000 & 0.000 \\
    */
  def outputLatexTable(x: Array[EvalResult], y: Array[EvalResult]): String = {
    x.zip(y).zipWithIndex.map{
      case ((a,b), idx) =>
          f"$$M${idx+1}$$" +
          f" & ${a.precision}%.3f & ${a.recall}%.3f & ${a.f}%.3f \t " +
          f" & ${b.precision}%.3f & ${b.recall}%.3f & ${b.f}%.3f \\\\"
    }.mkString("\n")
  }

  /**
    * 输出tikz绘制曲线图需要的数据格式
    *
    * @param x
    * @param y
    * @return
    */
  def outputPlotData(results: IndexedSeq[Array[EvalResult]], fromTopN: Int): String = {
    val precision = results.zipWithIndex
      .map{
          case (algorithms, idx) => f"${idx+fromTopN} " + algorithms.map(a=>f"${a.precision}%.3f").mkString(" ")
      }.mkString("\n")

    val recall = results.zipWithIndex
      .map{
        case (algorithms, idx) => f"${idx+fromTopN} " + algorithms.map(a=>f"${a.recall}%.3f").mkString(" ")
      }.mkString("\n")

    val fvalue = results.zipWithIndex
      .map{
        case (algorithms, idx) => f"${idx+fromTopN} " + algorithms.map(a=>f"${a.f}%.3f").mkString(" ")
      }.mkString("\n")

    val header = "TopN M1 M2 M3 M4 M5"
    Seq("Precision:", header, precision, "\nRecall:", header, recall, "\nF-value:", header, fvalue).mkString("\n")
  }
}
