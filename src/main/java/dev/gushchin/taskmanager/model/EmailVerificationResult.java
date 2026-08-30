package dev.gushchin.taskmanager.model;

public record EmailVerificationResult(User user, boolean alreadyVerified) {}
