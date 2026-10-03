package com.rolecompass.routing;

import com.rolecompass.dto.response.PredictionResponse.EliminatedRole;
import java.util.List;

/**
 * Immutable result returned by {@link AdaptiveRoutingEngine} after each evaluation.
 */
public record RoutingDecision(

        /** True when sufficient answers exist to call the ML prediction service. */
        boolean readyToPredict,

        /**
         * Surviving candidate role names after this routing step.
         * May be unchanged from the input if no elimination occurred.
         */
        List<String> survivingRoles,

        /**
         * IDs of questions selected for the next batch.
         * Empty when readyToPredict is true.
         */
        List<Long> nextQuestionIds,

        /**
         * The FSM state the session should transition to after this decision.
         * Persisted by SessionService onto the Session entity.
         */
        FsmState nextFsmState,

        /**
         * Human-readable explanation of the decision — used for logging/debugging only.
         * Never sent to React.
         */
        String reason,

        /**
         * Accumulated eliminated roles with student-facing reasons.
         */
        List<EliminatedRole> eliminationLog
) {
    /** Factory for a predict-ready decision. */
    public static RoutingDecision readyToPredict(List<String> survivors, FsmState nextState) {
        return readyToPredict(survivors, nextState, List.of());
    }

    public static RoutingDecision readyToPredict(List<String> survivors, FsmState nextState, List<EliminatedRole> eliminationLog) {
        return new RoutingDecision(true, survivors, List.of(), nextState, "Sufficient answers collected", eliminationLog != null ? eliminationLog : List.of());
    }

    /** Factory for a decision that continues assessment with more questions. */
    public static RoutingDecision continueWith(
            List<String> survivors,
            List<Long> nextQuestions,
            FsmState nextState,
            String reason
    ) {
        return continueWith(survivors, nextQuestions, nextState, reason, List.of());
    }

    public static RoutingDecision continueWith(
            List<String> survivors,
            List<Long> nextQuestions,
            FsmState nextState,
            String reason,
            List<EliminatedRole> eliminationLog
    ) {
        return new RoutingDecision(false, survivors, nextQuestions, nextState, reason, eliminationLog != null ? eliminationLog : List.of());
    }
}

