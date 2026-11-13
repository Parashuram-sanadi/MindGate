from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey, Date, UniqueConstraint, BigInteger
from sqlalchemy.orm import relationship
from datetime import datetime, timezone
from .database import Base

def utcnow():
    return datetime.now(timezone.utc)

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    password_hash = Column(String, nullable=False)
    role = Column(String, nullable=False, default="student") # student | parent | teacher
    role_selected = Column(Boolean, nullable=False, default=False)
    firebase_uid = Column(String, unique=True, index=True, nullable=True)
    created_at = Column(DateTime, default=utcnow)

    # Relationships
    device_registrations = relationship("DeviceRegistration", back_populates="user")

class ParentChildRelation(Base):
    __tablename__ = "parent_child_relation"
    id = Column(Integer, primary_key=True)
    parent_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    status = Column(String, nullable=False, default="active")  # active | revoked
    created_at = Column(DateTime, default=utcnow)
    linked_at = Column(DateTime, nullable=True)
    __table_args__ = (UniqueConstraint("parent_id", "student_id", name="uq_parent_student"),)

class ParentLinkToken(Base):
    __tablename__ = "parent_link_token"
    id = Column(Integer, primary_key=True)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    token_hash = Column(String, nullable=False, index=True)
    expires_at = Column(DateTime, nullable=False)
    used = Column(Boolean, default=False)
    attempt_count = Column(Integer, default=0)
    created_at = Column(DateTime, default=utcnow)

class DeviceRegistration(Base):
    __tablename__ = "device_registration"
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    device_id = Column(String, nullable=False)
    push_token = Column(String, nullable=False)
    platform = Column(String, default="android")
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=utcnow)
    updated_at = Column(DateTime, default=utcnow, onupdate=utcnow)

    user = relationship("User", back_populates="device_registrations")
    __table_args__ = (UniqueConstraint("user_id", "device_id", name="uq_user_device"),)

class UsageLimitEvent(Base):
    __tablename__ = "usage_limit_event"
    id = Column(Integer, primary_key=True)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    app_package = Column(String, nullable=True)
    limit_seconds = Column(Integer, nullable=False)
    actual_usage_seconds = Column(Integer, nullable=False)
    timestamp = Column(DateTime, nullable=False)
    event_date = Column(Date, nullable=False, index=True)
    synced_from_offline = Column(Boolean, default=False)
    created_at = Column(DateTime, default=utcnow)

class NotificationEvent(Base):
    __tablename__ = "notification_event"
    id = Column(Integer, primary_key=True)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    parent_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    event_type = Column(String, default="limit_exceeded")
    cooldown_bucket = Column(BigInteger, nullable=False)  # floor(event_epoch_seconds / 1200) — 20 min buckets
    sent_at = Column(DateTime, nullable=True)
    status = Column(String, default="pending")  # pending | sent | failed | suppressed_duplicate
    __table_args__ = (
        UniqueConstraint("student_id", "parent_id", "event_type", "cooldown_bucket", name="uq_dedup_key"),
    )

class ActiveLimitSession(Base):
    __tablename__ = "active_limit_session"
    id = Column(Integer, primary_key=True)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    app_package = Column(String, nullable=True)
    event_date = Column(Date, nullable=False)
    started_at = Column(DateTime, nullable=False)
    last_follow_up_sent_at = Column(DateTime, nullable=True)
    ended_at = Column(DateTime, nullable=True)
    __table_args__ = (UniqueConstraint("student_id", "app_package", "event_date", name="uq_active_session"),)

class ExtensionLedger(Base):
    __tablename__ = "extension_ledger"
    id = Column(Integer, primary_key=True)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    app_package = Column(String, nullable=False)
    extension_minutes = Column(Integer, nullable=False)
    granted_at = Column(DateTime, default=utcnow)
    date_key = Column(String, nullable=False, index=True) # YYYY-MM-DD
    source = Column(String, nullable=False) # puzzle | parent_approved
    tier = Column(Integer, default=1)

class ParentAlert(Base):
    __tablename__ = "parent_alerts"
    id = Column(Integer, primary_key=True)
    parent_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    type = Column(String, nullable=False)
    payload = Column(String) # JSON
    created_at = Column(DateTime, default=utcnow)
    read_at = Column(DateTime, nullable=True)

class PendingExtension(Base):
    __tablename__ = "pending_extensions"
    id = Column(Integer, primary_key=True)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    app_package = Column(String, nullable=False)
    requested_at = Column(DateTime, default=utcnow)
    status = Column(String, default="pending") # pending | approved | denied | timed_out

class BehaviorSignal(Base):
    __tablename__ = "behavior_signals"
    id = Column(Integer, primary_key=True)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    type = Column(String, nullable=False) # protection_disabled, state_mismatch, etc.
    timestamp = Column(DateTime, default=utcnow)

class UsageSnapshot(Base):
    __tablename__ = "usage_snapshot"
    id = Column(Integer, primary_key=True)
    student_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    app_package = Column(String, nullable=False)
    app_label = Column(String, nullable=False)
    used_seconds = Column(Integer, nullable=False)
    last_updated = Column(DateTime, default=utcnow, onupdate=utcnow)
    event_date = Column(Date, nullable=False, index=True)
    __table_args__ = (UniqueConstraint("student_id", "app_package", "event_date", name="uq_usage_snap"),)
