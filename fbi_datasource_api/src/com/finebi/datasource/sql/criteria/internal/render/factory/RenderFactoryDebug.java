package com.finebi.datasource.sql.criteria.internal.render.factory;

import com.finebi.datasource.sql.criteria.internal.CriteriaQueryImpl;
import com.finebi.datasource.sql.criteria.internal.QueryStructure;
import com.finebi.datasource.sql.criteria.internal.expression.*;
import com.finebi.datasource.sql.criteria.internal.expression.function.BasicFunctionExpression;
import com.finebi.datasource.sql.criteria.internal.path.AbstractFromImpl;
import com.finebi.datasource.sql.criteria.internal.path.AbstractPathImpl;
import com.finebi.datasource.sql.criteria.internal.path.RootImpl;
import com.finebi.datasource.sql.criteria.internal.predicate.*;
import com.finebi.datasource.sql.criteria.internal.render.LiteralRender;
import com.finebi.datasource.sql.criteria.internal.render.str.*;

/**
 * This class created on 2016/7/4.
 *
 * @author Connery
 * @since 4.0
 */
public class RenderFactoryDebug implements RenderFactory<LiteralRender> {
    public LiteralRender getQueryStructureRender(QueryStructure queryStructure, String driverTag) {
        return new QueryStructureRenderDebug(queryStructure);
    }

    @Override
    public LiteralRender getAbstractPathRender(AbstractPathImpl path, String driverTag) {
        return new AbstractPathRenderLiteral(path);
    }

    @Override
    public LiteralRender getAbstractFromRender(AbstractFromImpl from, String driverTag) {
        return new AbstractFromRenderLiteral(from);
    }

    @Override
    public LiteralRender getRootRender(RootImpl root, String driverTag) {
        return new RootLiteralRender(root);
    }

    @Override
    public LiteralRender getPathTypeExpressionRender(PathTypeExpression pathTypeExpression, String driverTag) {
        return new PathTypeExpressionRender(pathTypeExpression);
    }

    @Override
    public LiteralRender getBetweenPredicateRender(BetweenPredicate betweenPredicate, String driverTag) {
        return new BetweenPredicateLiteralRender(betweenPredicate);
    }

    @Override
    public LiteralRender getComparisonPredicateRender(ComparisonPredicate comparisonPredicate, String driverTag) {
        return new ComparisonPredicateLiteralRender(comparisonPredicate);
    }

    @Override
    public LiteralRender getBooleanAssertionPredicateRender(BooleanAssertionPredicate booleanAssertionPredicate, String driverTag) {
        return new BooleanAssertionPredicateLiteralRender(booleanAssertionPredicate);
    }

    @Override
    public LiteralRender getBooleanExpressionPredicateRender(BooleanExpressionPredicate booleanExpressionPredicate, String driverTag) {
        return new BooleanExpressionPredicateLiteralRender(booleanExpressionPredicate);
    }

    @Override
    public LiteralRender getBooleanStaticAssertionPredicateRender(BooleanStaticAssertionPredicate booleanStaticAssertionPredicate, String driverTag) {
        return new BooleanStaticAssertionPredicateLiteralRender(booleanStaticAssertionPredicate);
    }

    @Override
    public LiteralRender getCompoundPredicateRender(CompoundPredicate compoundPredicate, String driverTag) {
        return new CompoundPredicateLiteralRender(compoundPredicate);
    }

    @Override
    public LiteralRender getExistsPredicateRender(ExistsPredicate existsPredicate, String driverTag) {
        return new ExistsPredicateLiteralRender(existsPredicate);
    }

    @Override
    public LiteralRender getExplicitTruthValueCheckRender(ExplicitTruthValueCheck explicitTruthValueCheck, String driverTag) {
        return new ExplicitTruthValueCheckLiteralRender(explicitTruthValueCheck);
    }

    @Override
    public LiteralRender getInPredicateRender(InPredicate inPredicate, String driverTag) {
        return new InPredicateLiteralRender(inPredicate);
    }

    @Override
    public LiteralRender getLikePredicateRender(LikePredicate likePredicate, String driverTag) {
        return new LikePredicateLiteralRender(likePredicate);
    }

    @Override
    public LiteralRender getNegatedPredicateRender(NegatedPredicateWrapper negatedPredicateWrapper, String driverTag) {
        return new NegatedPredicateLiteralRender(negatedPredicateWrapper);
    }

    @Override
    public LiteralRender getNullnessPredicateRender(NullnessPredicate nullnessPredicate, String driverTag) {
        return new NullnessPredicateLiteralRender(nullnessPredicate);
    }

    @Override
    public LiteralRender getCriteriaQueryLiteralRender(CriteriaQueryImpl criteriaQuery, String driverTag) {
        return new CriteriaQueryLiteralRender(criteriaQuery);
    }

    @Override
    public LiteralRender getBasicFunctionExpressionLiteralRender(BasicFunctionExpression basicFunctionExpression, String driverTag) {
        return new BasicFunctionExpressionLiteralRender(basicFunctionExpression);
    }

    @Override
    public LiteralRender getBinaryArithmeticOperationLiteralRender(BinaryArithmeticOperation binaryArithmeticOperation, String driverTag) {
        return new BinaryArithmeticOperationLiteralRender(binaryArithmeticOperation);
    }

    @Override
    public LiteralRender getCoalesceExpressionLiteralRender(CoalesceExpression coalesceExpression, String driverTag) {
        return new CoalesceExpressionLiteralRender(coalesceExpression);
    }

    @Override
    public LiteralRender getConcatExpressionLiteralRender(ConcatExpression concatExpression, String driverTag) {
        return new ConcatExpressionLiteralRender(concatExpression);
    }

    @Override
    public LiteralRender getEntityTypeExpressionLiteralRender(EntityTypeExpression entityTypeExpression, String driverTag) {
        return new EntityTypeExpressionLiteralRender(entityTypeExpression);
    }

    @Override
    public LiteralRender getNullLiteralExpressionLiteralRender(NullLiteralExpression nullLiteralExpression, String driverTag) {
        return new NullLiteralExpressionLiteralRender(nullLiteralExpression);
    }

    @Override
    public LiteralRender getParameterExpressionImplLiteralRender(ParameterExpressionImpl parameterExpression, String driverTag) {
        return new ParameterExpressionImplLiteralRender(parameterExpression);
    }

    @Override
    public LiteralRender getPathTypeExpressionLiteralRender(PathTypeExpression PathTypeExpression, String driverTag) {
        return new PathTypeExpressionLiteralRender(PathTypeExpression) ;
    }

    @Override
    public LiteralRender getSimpleCaseExpressionLiteralRender(SimpleCaseExpression simpleCaseExpression, String driverTag) {
        return new SimpleCaseExpressionLiteralRender(simpleCaseExpression);
    }

    @Override
    public LiteralRender getSubqueryComparisonModifierExpressionLiteralRender(SubqueryComparisonModifierExpression subqueryComparisonModifierExpression, String driverTag) {
        return new SubqueryComparisonModifierExpressionLiteralRender(subqueryComparisonModifierExpression) ;
    }

    @Override
    public LiteralRender getUnaryArithmeticOperationLiteralRender(UnaryArithmeticOperation unaryArithmeticOperation, String driverTag) {
        return new UnaryArithmeticOperationLiteralRender(unaryArithmeticOperation);
    }

    @Override
    public LiteralRender getLiteralExpressionLiteralRender(LiteralExpression literalExpression, String driverTag) {
        return new LiteralExpressionLiteralRender(literalExpression);
    }

    @Override
    public LiteralRender getNullifExpressionLiteralRender(NullifExpression nullifExpression, String driverTag) {
        return new NullifExpressionLiteralRender(nullifExpression);
    }
}
