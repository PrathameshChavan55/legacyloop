package com.legacyloop.career.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * The career-side enums, together because they are small and always read as a group.
 *
 * <p>{@link ApplicationStatus} is the only one with behaviour: it owns the transition rules, so
 * "can this application move to SELECTED?" is answered next to the states rather than by an
 * if-chain in the service.
 */
public final class Enums {

    private Enums() {
    }

    public enum JobType {
        FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT;

        public String label() {
            return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
        }
    }

    public enum WorkMode { ONSITE, REMOTE, HYBRID }

    public enum JobStatus {
        DRAFT, OPEN, CLOSED;

        public String label() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    public enum ReferralStatus { REQUESTED, ACCEPTED, DECLINED, COMPLETED, CANCELLED }

    public enum AnalysisStatus { PENDING, COMPLETED, FAILED }

    public enum ApplicationStatus {

        APPLIED, UNDER_REVIEW, SHORTLISTED, REFERRED, INTERVIEW_SCHEDULED, SELECTED, REJECTED, WITHDRAWN;

        /** Ordered stages shown in the progress tracker. Rejection and withdrawal are exits, not stages. */
        private static final ApplicationStatus[] PIPELINE = {
                APPLIED, UNDER_REVIEW, SHORTLISTED, REFERRED, INTERVIEW_SCHEDULED, SELECTED};

        public boolean isTerminal() {
            return this == SELECTED || this == REJECTED || this == WITHDRAWN;
        }

        public boolean isSuccessful() {
            return this == SELECTED;
        }

        public String label() {
            return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
        }

        /** How far along the pipeline this status sits, as a percentage. */
        public int progress() {
            for (int i = 0; i < PIPELINE.length; i++) {
                if (PIPELINE[i] == this) {
                    return (i + 1) * 100 / PIPELINE.length;
                }
            }
            return this == WITHDRAWN || this == REJECTED ? 100 : 0;
        }

        /**
         * Where a reviewer may take this application next. A terminal status has nowhere to go,
         * which is what stops a selected candidate being quietly rejected afterwards.
         */
        public Set<ApplicationStatus> allowedNext() {
            return switch (this) {
                case APPLIED -> EnumSet.of(UNDER_REVIEW, SHORTLISTED, REJECTED);
                case UNDER_REVIEW -> EnumSet.of(SHORTLISTED, REFERRED, REJECTED);
                case SHORTLISTED -> EnumSet.of(INTERVIEW_SCHEDULED, REFERRED, SELECTED, REJECTED);
                case REFERRED -> EnumSet.of(INTERVIEW_SCHEDULED, SELECTED, REJECTED);
                case INTERVIEW_SCHEDULED -> EnumSet.of(SELECTED, REJECTED);
                case SELECTED, REJECTED, WITHDRAWN -> EnumSet.noneOf(ApplicationStatus.class);
            };
        }

        public boolean canMoveTo(ApplicationStatus target) {
            return allowedNext().contains(target);
        }
    }
}
