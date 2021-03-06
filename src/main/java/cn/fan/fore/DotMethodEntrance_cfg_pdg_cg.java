package cn.fan.fore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import cn.fan.cfg.assistant.MethodTransformer;
import cn.fan.model.AstNode;
import cn.fan.service.PaserClassToCfg;
import cn.fan.service.PaserSourceCodeToAst;
import cn.fan.tool.ExtractClassName;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

/**
 * cfg+pdg+call graph 主要引入就是方法体的调用图
 * 
 * @ClassName: DotMethodEntrance_cfg_pdg_cg
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author LiRongFan
 * @date 2019年1月15日 下午8:23:53
 *
 */
public class DotMethodEntrance_cfg_pdg_cg {
    public static void main(String[] args) {
        List<String> canIn = new ArrayList<String>();
        canIn.add("src");
        canIn.add("lib");
        List<String> notIn = new ArrayList<String>();
        notIn.add("bin");
        notIn.add("test");
        ExtractClassName extractClassName = new ExtractClassName(canIn, notIn);
        // 一个项目的所有java文件路径 已经入口package.className
        HashMap<String, String> paths = new HashMap<String, String>();
        // 该项目所依赖的jar
        List<String> jarsPath = new ArrayList<String>();
        File projectFile = new File("C:\\Users\\fan\\Desktop\\AST+CFG+PDG\\java-algorithms-implementation");
        // 在项目下面创建一个AST_CFG_PDGInfo目录放dot文件
        File aimPathFile = new File(projectFile.getAbsolutePath() + File.separator + "AST_CFG_PDGdotInfo");
        if (!aimPathFile.exists()) {
            aimPathFile.mkdir();
        }
        extractClassName.findJavaFile(projectFile, paths, jarsPath);
        for (Entry<String, String> pathToClassName : paths.entrySet()) {
            String className = pathToClassName.getKey();
            System.out.println("正在处理class:" + className);
            // 去除package前缀
            String name = className.substring(className.lastIndexOf(".") + 1);
            PaserSourceCodeToAst paserSourceCodeToAst = new PaserSourceCodeToAst(pathToClassName.getValue(), name + ".java");
            CompilationUnit cu = paserSourceCodeToAst.getCompilationUnit();
            AstNode root = new AstNode();
            AstNodeInit astNodeInit = new AstNodeInit(true, root);
            astNodeInit.Init(cu);
            // 入口方法
            List<String> methods = paserSourceCodeToAst.getMethodNames();

            System.out.println(methods);

            MethodTransformer methodTransformer = new MethodTransformer(methods);
            PaserClassToCfg<MethodTransformer> paserClassToCfg = new PaserClassToCfg<MethodTransformer>();
            // 需要解析的jar包和主入口类
            paserClassToCfg.parseCfg(methodTransformer, jarsPath, className);

            List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);
            for (MethodDeclaration methodDeclaration : methodDeclarations) {
                // 每个method 作为一个node 传入其中t
                if (className.equals("com.jwetherell.algorithms.data_structures.TrieMap")) {
                    System.out.println();
                }
                String methodFileName = getMethodFileName(methodDeclaration.getNameAsString(), methodDeclaration.getBegin().get().line, methodDeclaration.getParameters(),
                        methodTransformer.getMethod_ParaTypes());
                if (methodFileName == null) {
                    methodFileName = (className + "." + methodDeclaration.getNameAsString() + "_" + UUID.randomUUID()) + ".dot";
                }
                System.out.println(methodFileName);
            }

            // PaserClassToCfg_dot<MethodCfgTransformer> mCfg = new
            // PaserClassToCfg_dot<MethodCfgTransformer>();
            // mCfg.parseCfg(new
            // MethodCfgTransformer("C:\\Users\\fan\\Desktop\\AST+CFG+PDG\\java-algorithms-implementation"),
            // jarsPath, className);
        }
        System.out.println("dot文件全部生成完毕!");
    }

    /**
     * 找出ast正在解析的方法名字（因为重载的问题，所以需要判断）
     * 
     * @Title: getMethodFileName
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @author LiRongFan
     * @param methodName
     *            ast中获取的名字
     * @param parameters
     *            ast的类型
     * @param method_ParaTypes
     *            soot获取的class的方法名字等信息
     * @return
     * @throws
     */
    public static String getMethodFileName(String methodName, int lineNum, NodeList<Parameter> parameters, List<ArrayList<String>> method_ParaTypes) {
        ArrayList<Integer> indexs = new ArrayList<Integer>();
        // 先看看整个class中和当前方法名字一样的有几个
        for (int i = 0; i < method_ParaTypes.size(); i++) {
            if (method_ParaTypes.get(i).get(0).equals(methodName)) {
                indexs.add(i);
            }
        }
        // 只有一个方法名字对应
        if (indexs.size() == 1) {
            // 匹配一个删除一个
            String result = method_ParaTypes.get(indexs.get(0)).get(4);
            int index = indexs.get(0);
            method_ParaTypes.remove(index);
            return result;
        }
        else if (indexs.size() > 1) {
            // 看看参数的个数相同的有哪些
            ArrayList<Integer> same_param_num_indexs = new ArrayList<Integer>();
            for (Integer index : indexs) {
                if (Integer.valueOf(method_ParaTypes.get(index).get(1)) == parameters.size()) {
                    same_param_num_indexs.add(index);
                }
            }
            if (same_param_num_indexs.size() == 1) {
                String result = method_ParaTypes.get(same_param_num_indexs.get(0)).get(4);
                int index = same_param_num_indexs.get(0);
                method_ParaTypes.remove(index);
                return result;
            }
            else if (same_param_num_indexs.size() > 1) {
                // 参数个数一样，就要看是否顺序也相同
                HashMap<Integer, Integer> _same_param_type_size = new HashMap<Integer, Integer>();
                for (Integer index : same_param_num_indexs) {
                    String[] paratypes = method_ParaTypes.get(index).get(2).replace('[', ' ').replace(']', ' ').split(",");
                    for (int i = 0; i < paratypes.length; i++) {
                        if (parameters.get(i).getTypeAsString().trim().toLowerCase().length() == 1) {
                            continue;
                        }
                        if (parameters.get(i).getTypeAsString().trim().toLowerCase().contains(paratypes[i].toLowerCase().trim())
                                || paratypes[i].toLowerCase().trim().contains(parameters.get(i).getTypeAsString().trim().toLowerCase())) {
                            _same_param_type_size.put(index, _same_param_type_size.getOrDefault(index, 0) + 1);
                        }
                    }
                }
                if (_same_param_type_size.size() > 0) {
                    ArrayList<Entry<Integer, Integer>> entrys = new ArrayList<Map.Entry<Integer, Integer>>();
                    for (Entry<Integer, Integer> entry : _same_param_type_size.entrySet()) {
                        entrys.add(entry);
                    }
                    if (entrys.size() == 1) {
                        String result = method_ParaTypes.get(entrys.get(0).getKey()).get(4);
                        int index = entrys.get(0).getKey();
                        method_ParaTypes.remove(index);
                        return result;
                    }
                    entrys.sort((e1, e2) -> {
                        return e2.getValue() - e1.getValue();
                    });
                    // 只有一个类型顺序最匹配
                    if (entrys.get(0).getValue() > entrys.get(1).getValue()) {

                        String result = method_ParaTypes.get(entrys.get(0).getKey()).get(4);
                        int index = entrys.get(0).getKey();
                        method_ParaTypes.remove(index);
                        return result;
                    }
                    else {
                        // 留下来就是类型也相同 个数相同的
                        entrys.removeIf((entry) -> {
                            return entry.getValue() < entrys.get(0).getValue();
                        });
                        ArrayList<Integer[]> lines = new ArrayList<Integer[]>();
                        for (Entry<Integer, Integer> _index : entrys) {
                            lines.add(new Integer[] { _index.getKey(), lineNum - Integer.valueOf(method_ParaTypes.get(_index.getKey()).get(3)) });
                        }
                        // 按照行号相减的结果进行从小到大的排序
                        lines.sort((i1, i2) -> {
                            return i1[1] - i2[1];
                        });
                        String result = method_ParaTypes.get(lines.get(0)[0]).get(4);
                        int index = Integer.valueOf(lines.get(0)[0]);
                        method_ParaTypes.remove(index);
                        return result;
                    }
                }
                else {
                    ArrayList<Integer[]> lines = new ArrayList<Integer[]>();
                    for (int _index : same_param_num_indexs) {
                        lines.add(new Integer[] { _index, lineNum - Integer.valueOf(method_ParaTypes.get(_index).get(3)) });
                    }
                    // 按照行号相减的结果进行从小到大的排序
                    lines.sort((i1, i2) -> {
                        return i1[1] - i2[1];
                    });

                    String result = method_ParaTypes.get(lines.get(0)[0]).get(4);
                    int index = Integer.valueOf(lines.get(0)[0]);
                    method_ParaTypes.remove(index);
                    return result;
                }
            }
        }
        return null;
    }
}
